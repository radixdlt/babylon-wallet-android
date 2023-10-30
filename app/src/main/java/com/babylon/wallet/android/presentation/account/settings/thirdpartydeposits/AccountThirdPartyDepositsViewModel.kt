package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.settings.specificassets.DeleteDialogState
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.ret.Address
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.ManifestBuilderAddress
import com.radixdlt.ret.ManifestBuilderValue
import com.radixdlt.ret.NonFungibleGlobalId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.AddressValidator
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapWhen
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.extensions.toRETDepositRule
import rdx.works.profile.data.model.extensions.toRETResourcePreference
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.UpdateProfileThirdPartySettingsUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class AccountThirdPartyDepositsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val updateProfileThirdPartySettingsUseCase: UpdateProfileThirdPartySettingsUseCase,
    private val entityRepository: EntityRepository,
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
            val networkId = requireNotNull(state.value.account?.networkID)
            val manifestBuilder = BabylonManifestBuilder()
            val currentThirdPartyDeposits = state.value.account?.onLedgerSettings?.thirdPartyDeposits
            if (currentThirdPartyDeposits?.depositRule != state.value.updatedThirdPartyDepositSettings?.depositRule) {
                val depositRule = checkNotNull(state.value.updatedThirdPartyDepositSettings?.depositRule?.toRETDepositRule())
                manifestBuilder.setDefaultDepositRule(
                    accountAddress = Address(args.address),
                    accountDefaultDepositRule = depositRule
                )
            }
            val currentAssetExceptions = currentThirdPartyDeposits?.assetsExceptionList.orEmpty()
            val currentDepositors = currentThirdPartyDeposits?.depositorsAllowList.orEmpty()
            val newAssetExceptions = state.value.updatedThirdPartyDepositSettings?.assetsExceptionList.orEmpty()
            val newDepositors = state.value.updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty()
            currentAssetExceptions.minus(newAssetExceptions.toSet()).forEach { deletedException ->
                manifestBuilder.removeResourcePreference(Address(args.address), Address(deletedException.address))
            }
            newAssetExceptions.minus(currentAssetExceptions.toSet()).forEach { addedException ->
                manifestBuilder.setResourcePreference(
                    accountAddress = Address(args.address),
                    resourceAddress = Address(addedException.address),
                    preference = addedException.exceptionRule.toRETResourcePreference()
                )
            }
            currentDepositors.minus(newDepositors.toSet()).forEach { deletedDepositor ->
                manifestBuilder.removeAuthorizedDepositor(Address(args.address), deletedDepositor.toRETManifestBuilderValue())
            }
            newDepositors.minus(currentDepositors.toSet()).forEach { addedDepositor ->
                manifestBuilder.addAuthorizedDepositor(
                    accountAddress = Address(args.address),
                    depositorAddress = addedDepositor.toRETManifestBuilderValue(),
                )
            }
            manifestBuilder.buildSafely(networkId).onSuccess { manifest ->
                val updatedThirdPartyDepositSettings = state.value.updatedThirdPartyDepositSettings ?: return@onSuccess
                val requestId = UUIDGenerator.uuid().toString()
                incomingRequestRepository.add(
                    manifest.prepareInternalTransactionRequest(
                        networkId,
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
            val updatedDepositors = state.allowedDepositorsUiModels.filter {
                it != depositor
            }.toPersistentList()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositorsAllowList = updatedDepositors.mapNotNull { it.depositorAddress }
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
            state.value.assetExceptionsUiModels + listOf(assetExceptionToAdd)
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
        loadAssets(listOf(assetExceptionToAdd.assetException.address))
        checkIfSettingsChanged()
    }

    private fun loadAssets(addresses: List<String>) {
        viewModelScope.launch {
            entityRepository.getResources(addresses).onSuccess { resources ->
                val loadedResourcesAddresses = resources.map { it.resourceAddress }.toSet()
                _state.update { state ->
                    state.copy(
                        assetExceptionsUiModels = state.assetExceptionsUiModels.mapWhen(
                            predicate = {
                                loadedResourcesAddresses.contains(it.assetException.address)
                            }
                        ) { assetException ->
                            when (
                                val resource = resources.firstOrNull {
                                    it.resourceAddress == assetException.assetException.address
                                }
                            ) {
                                is Resource -> assetException.copy(resource = resource)
                                else -> assetException
                            }
                        }.toPersistentList(),
                        allowedDepositorsUiModels = state.allowedDepositorsUiModels.mapWhen(
                            predicate = {
                                loadedResourcesAddresses.contains(it.depositorAddress?.resourceAddress())
                            }
                        ) { depositor ->
                            when (
                                val resource = resources.firstOrNull {
                                    it.resourceAddress == depositor.depositorAddress?.resourceAddress()
                                }
                            ) {
                                is Resource -> depositor.copy(resource = resource)
                                else -> depositor
                            }
                        }.toPersistentList(),
                    )
                }
            }
        }
    }

    fun onAddDepositor() {
        state.value.depositorToAdd.depositorAddress?.let { depositorAddress ->
            _state.update { state ->
                state.depositorToAdd.depositorAddress?.let { depositorAddress ->
                    val updatedDepositors =
                        (state.allowedDepositorsUiModels + listOf(AssetType.Depositor(depositorAddress = depositorAddress))).distinct()
                    state.copy(
                        updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                            depositorsAllowList = updatedDepositors.mapNotNull { it.depositorAddress }
                        ),
                        depositorToAdd = AssetType.Depositor(),
                        allowedDepositorsUiModels = updatedDepositors.toPersistentList()
                    )
                } ?: state
            }
            loadAssets(listOf(depositorAddress.resourceAddress()))
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
            val updateAssetExceptions = state.assetExceptionsUiModels.filter { it.assetException != asset }
            state.copy(
                assetExceptionsUiModels = updateAssetExceptions.toPersistentList(),
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updateAssetExceptions.map { it.assetException }
                ),
                deleteDialogState = DeleteDialogState.None
            )
        }
        checkIfSettingsChanged()
    }

    fun assetExceptionAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val valid = AddressValidator.isValidForTypes(
            address = address,
            networkId = currentNetworkId,
            allowedEntityTypes = setOf(EntityType.GLOBAL_FUNGIBLE_RESOURCE_MANAGER, EntityType.GLOBAL_NON_FUNGIBLE_RESOURCE_MANAGER)
        )
        val alreadyAdded = state.value.assetExceptionsUiModels.any { it.assetException.address == address }

        _state.update { state ->
            val updatedException = state.assetExceptionToAdd.copy(
                assetException = state.assetExceptionToAdd.assetException.copy(address = address),
                addressValid = valid && !alreadyAdded
            )
            state.copy(assetExceptionToAdd = updatedException)
        }
        checkIfSettingsChanged()
    }

    fun depositorAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val validAddress = AddressValidator.isValidForTypes(
            address = address,
            networkId = currentNetworkId,
            allowedEntityTypes = setOf(EntityType.GLOBAL_FUNGIBLE_RESOURCE_MANAGER, EntityType.GLOBAL_NON_FUNGIBLE_RESOURCE_MANAGER)
        )
        val validNft = AddressValidator.isValidNft(address)
        _state.update { state ->
            val updatedDepositor = state.depositorToAdd.copy(
                depositorAddress = when {
                    validAddress -> ThirdPartyDeposits.DepositorAddress.ResourceAddress(address)
                    validNft -> ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(address)
                    else -> null
                },
                addressValid = validAddress || validNft,
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
            getProfileUseCase.accountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address } }
                .collect { account ->
                    _state.update { state ->
                        state.copy(
                            account = account,
                            updatedThirdPartyDepositSettings = account.onLedgerSettings.thirdPartyDeposits,
                            assetExceptionsUiModels = account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList.map {
                                AssetType.AssetException(assetException = it)
                            }.toPersistentList(),
                            allowedDepositorsUiModels = account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList.map {
                                AssetType.Depositor(it)
                            }.toPersistentList()
                        )
                    }
                    checkIfSettingsChanged()
                    loadAssets(
                        (
                            account.onLedgerSettings.thirdPartyDeposits.assetsExceptionList.map {
                                it.address
                            } + account.onLedgerSettings.thirdPartyDeposits.depositorsAllowList.map {
                                it.resourceAddress()
                            }
                            ).distinct()
                    )
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
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
    val accountAddress: String,
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
    val assetExceptionsUiModels: ImmutableList<AssetType.AssetException> = persistentListOf(),
    val allowedDepositorsUiModels: ImmutableList<AssetType.Depositor> = persistentListOf()
) : UiState {
    val allowedAssets: PersistentList<AssetType.AssetException>
        get() = assetExceptionsUiModels.filter {
            it.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        }.toPersistentList()

    val deniedAssets: PersistentList<AssetType.AssetException>
        get() = assetExceptionsUiModels.filter {
            it.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Deny
        }.toPersistentList()
}

fun ThirdPartyDeposits.DepositorAddress.toRETManifestBuilderValue(): ManifestBuilderValue {
    return when (this) {
        is ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID -> {
            val nonFungibleGlobalId = NonFungibleGlobalId(address)
            ManifestBuilderValue.EnumValue(
                0u,
                listOf(
                    ManifestBuilderValue.TupleValue(
                        listOf(
                            ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(nonFungibleGlobalId.resourceAddress())),
                            ManifestBuilderValue.NonFungibleLocalIdValue(nonFungibleGlobalId.localId())
                        )
                    )
                )
            )
        }

        is ThirdPartyDeposits.DepositorAddress.ResourceAddress -> {
            val retAddress = Address(address)
            ManifestBuilderValue.EnumValue(
                1u,
                fields = listOf(ManifestBuilderValue.AddressValue(ManifestBuilderAddress.Static(retAddress)))
            )
        }
    }
}

fun ThirdPartyDeposits.DepositorAddress.resourceAddress(): String {
    return when (this) {
        is ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID -> NonFungibleGlobalId(address).resourceAddress().addressString()
        is ThirdPartyDeposits.DepositorAddress.ResourceAddress -> address
    }
}
