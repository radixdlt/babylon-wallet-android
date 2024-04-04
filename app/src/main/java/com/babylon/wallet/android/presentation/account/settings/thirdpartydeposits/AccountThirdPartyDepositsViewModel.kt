package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.presentation.account.settings.specificassets.DeleteDialogState
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.thirdPartyDepositUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.validatedOnNetworkOrNull
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.UpdateProfileThirdPartySettingsUseCase
import rdx.works.profile.domain.activeAccountsOnCurrentNetwork
import rdx.works.profile.sargon.toSargon
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class AccountThirdPartyDepositsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val updateProfileThirdPartySettingsUseCase: UpdateProfileThirdPartySettingsUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountThirdPartyDepositsUiState>() {

    private var pollJob: Job? = null
    private val args = AccountThirdPartyDepositsArgs(savedStateHandle)

    override fun initialState(): AccountThirdPartyDepositsUiState = AccountThirdPartyDepositsUiState(accountAddress = args.address)

    init {
        loadAccount()
    }

    private fun handleRequestStatus(requestId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            transactionStatusClient.listenForPollStatusByRequestId(requestId).collect { status ->
                status.result.onSuccess {
                    when (val type = status.transactionType) {
                        is TransactionType.UpdateThirdPartyDeposits -> {
                            val account = requireNotNull(state.value.account)
                            updateProfileThirdPartySettingsUseCase(account, type.thirdPartyDeposits)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    fun onAllowAll() {
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositRule = ThirdPartyDeposits.DepositRule.AcceptAll
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onDenyAll() {
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositRule = ThirdPartyDeposits.DepositRule.DenyAll
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onAcceptKnown() {
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositRule = ThirdPartyDeposits.DepositRule.AcceptKnown
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onUpdateThirdPartyDeposits() {
        viewModelScope.launch {
            val currentThirdPartyDeposits = state.value.account?.onLedgerSettings?.thirdPartyDeposits ?: return@launch

            val newDepositRule = state.value.updatedThirdPartyDepositSettings?.depositRule ?: currentThirdPartyDeposits.depositRule
            val newAssetExceptions = state.value.updatedThirdPartyDepositSettings?.assetsExceptionList.orEmpty()
            val newDepositors = state.value.updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty()

            runCatching {
                TransactionManifest.thirdPartyDepositUpdate(
                    accountAddress = args.address,
                    from = com.radixdlt.sargon.ThirdPartyDeposits(
                        depositRule = currentThirdPartyDeposits.depositRule.toSargon(),
                        assetsExceptionList = currentThirdPartyDeposits.assetsExceptionList?.map { it.toSargon() }.orEmpty(),
                        depositorsAllowList = currentThirdPartyDeposits.depositorsAllowList?.map { it.toSargon() }.orEmpty()
                    ),
                    to = com.radixdlt.sargon.ThirdPartyDeposits(
                        depositRule = newDepositRule.toSargon(),
                        assetsExceptionList = newAssetExceptions.map { it.toSargon() },
                        depositorsAllowList = newDepositors.map { it.toSargon() }
                    )
                )
            }.mapCatching {
                TransactionManifestData.from(it)
            }.onSuccess { manifest ->
                val updatedThirdPartyDepositSettings = state.value.updatedThirdPartyDepositSettings ?: return@onSuccess
                val requestId = UUIDGenerator.uuid().toString()
                incomingRequestRepository.add(
                    manifest.prepareInternalTransactionRequest(
                        requestId = requestId,
                        transactionType = TransactionType.UpdateThirdPartyDeposits(updatedThirdPartyDepositSettings),
                        blockUntilCompleted = true
                    )
                )
                handleRequestStatus(requestId)
            }.onFailure { t ->
                _state.update { state ->
                    state.copy(error = UiMessage.ErrorMessage(t))
                }
            }
        }
    }

    fun onDeleteDepositor(depositor: AssetType.Depositor) {
        _state.update { state ->
            val updatedDepositors = state.allowedDepositorsUiModels?.filter {
                it != depositor
            }?.toPersistentList()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositorsAllowList = updatedDepositors?.mapNotNull { it.depositorAddress }
                ),
                deleteDialogState = DeleteDialogState.None,
                allowedDepositorsUiModels = updatedDepositors
            )
        }
        checkIfSettingsChanged()
    }

    fun onAddAssetException() {
        val assetExceptionToAdd = state.value.assetExceptionToAdd
        val updatedAssetExceptionsUiModels = (
            state.value.assetExceptionsUiModels.orEmpty() + listOf(assetExceptionToAdd)
            ).toPersistentList()
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updatedAssetExceptionsUiModels.map { it.assetException }
                ),
                assetExceptionsUiModels = updatedAssetExceptionsUiModels,
                assetExceptionToAdd = AssetType.AssetException()
            )
        }
        loadAssets(setOf(ResourceAddress.init(assetExceptionToAdd.assetException.address)))
        checkIfSettingsChanged()
    }

    private fun loadAssets(addresses: Set<ResourceAddress>) = viewModelScope.launch {
        getResourcesUseCase(addresses = addresses).onSuccess { resources ->
            val loadedResourcesAddresses = resources.map { it.address }.toSet()
            _state.update { state ->
                state.copy(
                    assetExceptionsUiModels = state.assetExceptionsUiModels?.mapWhen(
                        predicate = {
                            loadedResourcesAddresses.contains(ResourceAddress.init(it.assetException.address))
                        }
                    ) { assetException ->
                        when (
                            val resource = resources.firstOrNull {
                                it.address.string == assetException.assetException.address
                            }
                        ) {
                            is Resource -> assetException.copy(resource = resource)
                            else -> assetException
                        }
                    }?.toPersistentList(),
                    allowedDepositorsUiModels = state.allowedDepositorsUiModels?.mapWhen(
                        predicate = {
                            loadedResourcesAddresses.contains(it.depositorAddress?.resourceAddress())
                        }
                    ) { depositor ->
                        when (
                            val resource = resources.firstOrNull {
                                it.address == depositor.depositorAddress?.resourceAddress()
                            }
                        ) {
                            is Resource -> depositor.copy(resource = resource)
                            else -> depositor
                        }
                    }?.toPersistentList(),
                )
            }
        }
    }

    fun onAddDepositor() {
        state.value.depositorToAdd.depositorAddress?.let { depositorAddress ->
            _state.update { state ->
                state.depositorToAdd.depositorAddress?.let { depositorAddress ->
                    val allowedDepositorsUiModels = state.allowedDepositorsUiModels.orEmpty()
                    val newDepositors = listOf(AssetType.Depositor(depositorAddress = depositorAddress))
                    val updatedDepositors = (allowedDepositorsUiModels + newDepositors).distinct()
                    state.copy(
                        updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                            depositorsAllowList = updatedDepositors.mapNotNull { it.depositorAddress }
                        ),
                        depositorToAdd = AssetType.Depositor(),
                        allowedDepositorsUiModels = updatedDepositors.toPersistentList()
                    )
                } ?: state
            }
            loadAssets(setOf(depositorAddress.resourceAddress()))
        }
        checkIfSettingsChanged()
    }

    fun onAssetExceptionRuleChanged(rule: ThirdPartyDeposits.DepositAddressExceptionRule) {
        _state.update { state ->
            state.copy(
                assetExceptionToAdd = state.assetExceptionToAdd.copy(
                    assetException = state.assetExceptionToAdd.assetException.copy(
                        exceptionRule = rule
                    )
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun showDeletePrompt(asset: ThirdPartyDeposits.AssetException) {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.AboutToDeleteAssetException(asset))
        }
    }

    fun showDeletePrompt(depositor: AssetType.Depositor) {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.AboutToDeleteAssetDepositor(depositor))
        }
    }

    fun hideDeletePrompt() {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.None)
        }
    }

    fun onDeleteAsset(asset: ThirdPartyDeposits.AssetException?) {
        _state.update { state ->
            val updateAssetExceptions = state.assetExceptionsUiModels?.filter { it.assetException != asset }
            state.copy(
                assetExceptionsUiModels = updateAssetExceptions?.toPersistentList(),
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updateAssetExceptions?.map { it.assetException }
                ),
                deleteDialogState = DeleteDialogState.None
            )
        }
        checkIfSettingsChanged()
    }

    fun assetExceptionAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val isAddressValid = ResourceAddress.validatedOnNetworkOrNull(
            validating = address,
            networkId = NetworkId.init(discriminant = currentNetworkId.toUByte())
        ) != null
        val alreadyAdded = state.value.assetExceptionsUiModels?.any { it.assetException.address == address } == true

        _state.update { state ->
            val updatedException = state.assetExceptionToAdd.copy(
                assetException = state.assetExceptionToAdd.assetException.copy(address = address),
                addressValid = isAddressValid && !alreadyAdded
            )
            state.copy(assetExceptionToAdd = updatedException)
        }
        checkIfSettingsChanged()
    }

    fun depositorAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val validatedResourceAddress = ResourceAddress.validatedOnNetworkOrNull(
            validating = address,
            networkId = NetworkId.init(discriminant = currentNetworkId.toUByte())
        )
        val validatedNftAddress = NonFungibleGlobalId.validatedOnNetworkOrNull(
            validating = address,
            networkId = NetworkId.init(discriminant = currentNetworkId.toUByte())
        )

        val badgeAddress = if (validatedResourceAddress != null) {
            ResourceOrNonFungible.Resource(validatedResourceAddress)
        } else if (validatedNftAddress != null) {
            ResourceOrNonFungible.NonFungible(validatedNftAddress)
        } else {
            null
        }

        _state.update { state ->
            val updatedDepositor = state.depositorToAdd.copy(
                depositorAddress = when (badgeAddress) {
                    is ResourceOrNonFungible.Resource -> ThirdPartyDeposits.DepositorAddress.ResourceAddress(badgeAddress.value.string)
                    is ResourceOrNonFungible.NonFungible -> ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(
                        badgeAddress.value.string
                    )
                    else -> null
                },
                addressValid = badgeAddress != null,
                addressToDisplay = address
            )
            state.copy(depositorToAdd = updatedDepositor)
        }
        checkIfSettingsChanged()
    }

    private fun checkIfSettingsChanged() {
        _state.update { state ->
            state.copy(canUpdate = state.updatedThirdPartyDepositSettings != state.account?.onLedgerSettings?.thirdPartyDeposits)
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.activeAccountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address.string } }
                .collect { account ->
                    _state.update { state ->
                        state.copy(
                            account = account,
                            updatedThirdPartyDepositSettings = account.onLedgerSettings.thirdPartyDeposits,
                            assetExceptionsUiModels = account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList?.map {
                                AssetType.AssetException(assetException = it)
                            }?.toPersistentList(),
                            allowedDepositorsUiModels = account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList?.map {
                                AssetType.Depositor(it)
                            }?.toPersistentList()
                        )
                    }
                    checkIfSettingsChanged()
                    loadAssets(
                        addresses = account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList?.map {
                            ResourceAddress.init(it.address)
                        }.orEmpty().toSet() + account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList?.map {
                            it.resourceAddress()
                        }.orEmpty().toSet()
                    )
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun setAddAssetSheetVisible(isVisible: Boolean) {
        _state.update { it.copy(selectedSheetState = if (isVisible) SelectedDepositsSheetState.AddAsset else null) }
    }

    fun setAddDepositorSheetVisible(isVisible: Boolean) {
        _state.update { it.copy(selectedSheetState = if (isVisible) SelectedDepositsSheetState.AddDepositor else null) }
    }
}

sealed class AssetType {
    data class AssetException(
        val assetException: ThirdPartyDeposits.AssetException = ThirdPartyDeposits.AssetException(
            address = "",
            exceptionRule = ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        ),
        val resource: Resource? = null,
        val addressValid: Boolean = false
    )

    data class Depositor(
        val depositorAddress: ThirdPartyDeposits.DepositorAddress? = null,
        val resource: Resource? = null,
        val addressValid: Boolean = false,
        val addressToDisplay: String = ""
    )
}

data class AccountThirdPartyDepositsUiState(
    val account: Network.Account? = null,
    val accountAddress: AccountAddress,
    val updatedThirdPartyDepositSettings: ThirdPartyDeposits? = null,
    val canUpdate: Boolean = false,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val error: UiMessage? = null,
    val assetExceptionToAdd: AssetType.AssetException = AssetType.AssetException(
        ThirdPartyDeposits.AssetException(
            "",
            ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        )
    ),
    val depositorToAdd: AssetType.Depositor = AssetType.Depositor(),
    val assetExceptionsUiModels: ImmutableList<AssetType.AssetException>? = persistentListOf(),
    val allowedDepositorsUiModels: ImmutableList<AssetType.Depositor>? = persistentListOf(),
    val selectedSheetState: SelectedDepositsSheetState? = null
) : UiState {

    val isAddAssetSheetVisible: Boolean
        get() = selectedSheetState is SelectedDepositsSheetState.AddAsset

    val isAddDepositorSheetVisible: Boolean
        get() = selectedSheetState is SelectedDepositsSheetState.AddDepositor

    val allowedAssets: PersistentList<AssetType.AssetException>?
        get() = assetExceptionsUiModels?.filter {
            it.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        }?.toPersistentList()

    val deniedAssets: PersistentList<AssetType.AssetException>?
        get() = assetExceptionsUiModels?.filter {
            it.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Deny
        }?.toPersistentList()
}

sealed interface SelectedDepositsSheetState {
    data object AddAsset : SelectedDepositsSheetState
    data object AddDepositor : SelectedDepositsSheetState
}

fun ThirdPartyDeposits.DepositorAddress.resourceAddress(): ResourceAddress {
    return when (this) {
        is ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID -> ResourceOrNonFungible.NonFungible(
            NonFungibleGlobalId.init(address)
        ).value.resourceAddress

        is ThirdPartyDeposits.DepositorAddress.ResourceAddress -> ResourceOrNonFungible.Resource(
            ResourceAddress.init(address)
        ).value
    }
}
