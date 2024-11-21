package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.interaction.PrepareInternalTransactionUseCase
import com.babylon.wallet.android.presentation.account.settings.specificassets.DeleteDialogState
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AssetException
import com.radixdlt.sargon.DepositAddressExceptionRule
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.AssetsExceptionList
import com.radixdlt.sargon.extensions.DepositorsAllowList
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.thirdPartyDepositUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.validatedOnNetworkOrNull
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.resourceAddress
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.UpdateProfileThirdPartySettingsUseCase
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountThirdPartyDepositsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val updateProfileThirdPartySettingsUseCase: UpdateProfileThirdPartySettingsUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val prepareInternalTransactionUseCase: PrepareInternalTransactionUseCase,
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
                    depositRule = DepositRule.ACCEPT_ALL
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onDenyAll() {
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositRule = DepositRule.DENY_ALL
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onAcceptKnown() {
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositRule = DepositRule.ACCEPT_KNOWN
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onUpdateThirdPartyDeposits() {
        viewModelScope.launch {
            val currentThirdPartyDeposits = state.value.account?.onLedgerSettings?.thirdPartyDeposits ?: return@launch
            val updatedThirdPartyDepositSettings = state.value.updatedThirdPartyDepositSettings ?: return@launch

            val newDepositRule = state.value.updatedThirdPartyDepositSettings?.depositRule ?: currentThirdPartyDeposits.depositRule
            val newAssetExceptions = state.value.updatedThirdPartyDepositSettings?.assetsExceptionList.orEmpty()
            val newDepositors = state.value.updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty()

            runCatching {
                TransactionManifest.thirdPartyDepositUpdate(
                    accountAddress = args.address,
                    from = ThirdPartyDeposits(
                        depositRule = currentThirdPartyDeposits.depositRule,
                        assetsExceptionList = currentThirdPartyDeposits.assetsExceptionList,
                        depositorsAllowList = currentThirdPartyDeposits.depositorsAllowList
                    ),
                    to = ThirdPartyDeposits(
                        depositRule = newDepositRule,
                        assetsExceptionList = AssetsExceptionList(newAssetExceptions).asList(),
                        depositorsAllowList = DepositorsAllowList(newDepositors).asList()
                    )
                )
            }.mapCatching {
                UnvalidatedManifestData.from(it)
            }.then { manifest ->
                val requestId = UUID.randomUUID().toString()

                prepareInternalTransactionUseCase(
                    unvalidatedManifestData = manifest,
                    requestId = requestId,
                    transactionType = TransactionType.UpdateThirdPartyDeposits(updatedThirdPartyDepositSettings),
                    blockUntilCompleted = true
                )
            }.onSuccess { transactionRequest ->
                incomingRequestRepository.add(transactionRequest)
                handleRequestStatus(transactionRequest.interactionId)
            }.onFailure { t ->
                _state.update { state ->
                    state.copy(error = UiMessage.ErrorMessage(t))
                }
            }
        }
    }

    fun onDeleteDepositor(depositor: AssetType.DepositorType) {
        _state.update { state ->
            val updatedDepositors = state.allowedDepositorsUiModels?.filter {
                it != depositor
            }?.toPersistentList()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositorsAllowList = DepositorsAllowList(updatedDepositors?.mapNotNull { it.depositorAddress }.orEmpty()).asList()
                ),
                deleteDialogState = DeleteDialogState.None,
                allowedDepositorsUiModels = updatedDepositors
            )
        }
        checkIfSettingsChanged()
    }

    private fun loadAssets(addresses: Set<ResourceOrNonFungible>) = viewModelScope.launch {
        resolveAssetsFromAddressUseCase(addresses).onSuccess { assets ->
            _state.update { state ->
                state.copy(
                    assetExceptionsUiModels = state.assetExceptionsUiModels?.map { model ->
                        val asset = assets.find { it.resource.address == model.assetAddress }

                        if (asset != null) {
                            model.copy(resource = asset.resource)
                        } else {
                            model
                        }
                    }?.toPersistentList(),
                    allowedDepositorsUiModels = state.allowedDepositorsUiModels?.map { model ->
                        val asset = assets.find { it.resource.address == model.depositorAddress?.resourceAddress }

                        if (asset != null) {
                            model.copy(resource = asset.resource)
                        } else {
                            model
                        }
                    }?.toPersistentList()
                )
            }
        }
    }

    fun onAddAssetException() {
        val assetExceptionToAdd = state.value.assetExceptionToAdd
        val updatedAssetExceptionsUiModels = (state.value.assetExceptionsUiModels.orEmpty() + listOf(assetExceptionToAdd))
            .toPersistentList()
        _state.update { state ->
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = AssetsExceptionList(updatedAssetExceptionsUiModels.mapNotNull { it.assetException }).asList()
                ),
                assetExceptionsUiModels = updatedAssetExceptionsUiModels,
                assetExceptionToAdd = AssetType.ExceptionType()
            )
        }
        if (assetExceptionToAdd.assetAddress != null) {
            loadAssets(setOf(ResourceOrNonFungible.Resource(assetExceptionToAdd.assetAddress)))
        }
        checkIfSettingsChanged()
    }

    fun onAddDepositor() {
        state.value.depositorToAdd.depositorAddress?.let { depositorAddress ->
            _state.update { state ->
                state.depositorToAdd.depositorAddress?.let { depositorAddress ->
                    val allowedDepositorsUiModels = state.allowedDepositorsUiModels.orEmpty()
                    val newDepositors = listOf(AssetType.DepositorType(depositorAddress = depositorAddress))
                    val updatedDepositors = (allowedDepositorsUiModels + newDepositors).distinct()
                    state.copy(
                        updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                            depositorsAllowList = DepositorsAllowList(updatedDepositors.mapNotNull { it.depositorAddress }).asList()
                        ),
                        depositorToAdd = AssetType.DepositorType(),
                        allowedDepositorsUiModels = updatedDepositors.toPersistentList()
                    )
                } ?: state
            }
            loadAssets(setOf(depositorAddress))
        }
        checkIfSettingsChanged()
    }

    fun onAssetExceptionRuleChanged(rule: DepositAddressExceptionRule) {
        _state.update { state ->
            state.copy(assetExceptionToAdd = state.assetExceptionToAdd.copy(rule = rule))
        }
        checkIfSettingsChanged()
    }

    fun showDeletePrompt(asset: AssetType.ExceptionType) {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.AboutToDeleteAssetException(asset))
        }
    }

    fun showDeletePrompt(depositor: AssetType.DepositorType) {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.AboutToDeleteAssetDepositor(depositor))
        }
    }

    fun hideDeletePrompt() {
        _state.update { state ->
            state.copy(deleteDialogState = DeleteDialogState.None)
        }
    }

    fun onDeleteAsset(asset: AssetType.ExceptionType) {
        _state.update { state ->
            val updateAssetExceptions = state.assetExceptionsUiModels?.filterNot { it.assetException == asset.assetException }
            state.copy(
                assetExceptionsUiModels = updateAssetExceptions?.toPersistentList(),
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = AssetsExceptionList(updateAssetExceptions?.mapNotNull { it.assetException }.orEmpty()).asList()
                ),
                deleteDialogState = DeleteDialogState.None
            )
        }
        checkIfSettingsChanged()
    }

    fun assetExceptionAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkId ?: return
        val alreadyAdded = state.value.assetExceptionsUiModels?.any { it.assetException?.address?.string == address } == true
        val validatedAddress = ResourceAddress.validatedOnNetworkOrNull(validating = address, networkId = currentNetworkId)

        _state.update { state ->
            val updatedException = state.assetExceptionToAdd.copy(
                assetAddress = validatedAddress,
                addressValid = validatedAddress != null && !alreadyAdded,
                addressToDisplay = address
            )
            state.copy(assetExceptionToAdd = updatedException)
        }
        checkIfSettingsChanged()
    }

    fun depositorAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkId ?: return
        val validatedResourceAddress = ResourceAddress.validatedOnNetworkOrNull(
            validating = address,
            networkId = currentNetworkId
        )
        val validatedNftAddress = NonFungibleGlobalId.validatedOnNetworkOrNull(
            validating = address,
            networkId = currentNetworkId
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
                depositorAddress = badgeAddress,
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
            getProfileUseCase.flow.mapNotNull { profile ->
                profile.activeAccountOnCurrentNetwork(withAddress = args.address)
            }.collect { account ->
                _state.update { state ->
                    state.copy(
                        account = account,
                        updatedThirdPartyDepositSettings = account.onLedgerSettings.thirdPartyDeposits,
                        assetExceptionsUiModels = account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList?.map {
                            AssetType.ExceptionType(
                                assetAddress = it.address,
                                rule = it.exceptionRule
                            )
                        }?.toPersistentList(),
                        allowedDepositorsUiModels = account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList?.map {
                            AssetType.DepositorType(depositorAddress = it)
                        }?.toPersistentList()
                    )
                }
                checkIfSettingsChanged()

                val assetExceptionAddresses = account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList.orEmpty().map {
                    ResourceOrNonFungible.Resource(it.address)
                }.toSet()
                val depositorAddresses = account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList.orEmpty().toSet()

                loadAssets(addresses = assetExceptionAddresses + depositorAddresses)
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun setAddAssetSheetVisible(isVisible: Boolean) {
        _state.update {
            it.copy(
                selectedSheetState = if (isVisible) SelectedDepositsSheetState.AddAsset else null
            )
        }
    }

    fun setAddDepositorSheetVisible(isVisible: Boolean) {
        _state.update {
            it.copy(
                selectedSheetState = if (isVisible) SelectedDepositsSheetState.AddDepositor else null,
                depositorToAdd = if (isVisible) AssetType.DepositorType() else it.depositorToAdd
            )
        }
    }
}

sealed class AssetType {
    data class ExceptionType(
        val assetAddress: ResourceAddress? = null,
        val rule: DepositAddressExceptionRule = DepositAddressExceptionRule.ALLOW,
        val resource: Resource? = null,
        val addressValid: Boolean = false,
        val addressToDisplay: String = ""
    ) {

        val assetException: AssetException?
            get() = if (assetAddress != null) {
                AssetException(
                    address = assetAddress,
                    exceptionRule = rule
                )
            } else {
                null
            }
    }

    data class DepositorType(
        val depositorAddress: ResourceOrNonFungible? = null,
        val resource: Resource? = null,
        val addressValid: Boolean = false,
        val addressToDisplay: String = ""
    )
}

data class AccountThirdPartyDepositsUiState(
    val account: Account? = null,
    val accountAddress: AccountAddress,
    val updatedThirdPartyDepositSettings: ThirdPartyDeposits? = null,
    val canUpdate: Boolean = false,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val error: UiMessage? = null,
    val assetExceptionToAdd: AssetType.ExceptionType = AssetType.ExceptionType(),
    val depositorToAdd: AssetType.DepositorType = AssetType.DepositorType(),
    val assetExceptionsUiModels: ImmutableList<AssetType.ExceptionType>? = persistentListOf(),
    val allowedDepositorsUiModels: ImmutableList<AssetType.DepositorType>? = persistentListOf(),
    val selectedSheetState: SelectedDepositsSheetState? = null
) : UiState {

    val isAddAssetSheetVisible: Boolean
        get() = selectedSheetState is SelectedDepositsSheetState.AddAsset

    val isAddDepositorSheetVisible: Boolean
        get() = selectedSheetState is SelectedDepositsSheetState.AddDepositor

    val allowedAssets: PersistentList<AssetType.ExceptionType>?
        get() = assetExceptionsUiModels?.filter {
            it.assetException?.exceptionRule == DepositAddressExceptionRule.ALLOW
        }?.toPersistentList()

    val deniedAssets: PersistentList<AssetType.ExceptionType>?
        get() = assetExceptionsUiModels?.filter {
            it.assetException?.exceptionRule == DepositAddressExceptionRule.DENY
        }?.toPersistentList()
}

sealed interface SelectedDepositsSheetState {
    data object AddAsset : SelectedDepositsSheetState
    data object AddDepositor : SelectedDepositsSheetState
}
