package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.account.specificassets.DeleteDialogState
import com.radixdlt.ret.Address
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.ManifestBuilderAddress
import com.radixdlt.ret.ManifestBuilderValue
import com.radixdlt.ret.NonFungibleGlobalId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.AddressValidator
import rdx.works.core.UUIDGenerator
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.data.utils.toRETDepositRule
import rdx.works.profile.data.utils.toRETResourcePreference
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
                    state.copy(error = UiMessage.ErrorMessage.from(t))
                }
            }
        }
    }

    fun onDeleteDepositor(depositor: ThirdPartyDeposits.DepositorAddress) {
        _state.update { state ->
            val updatedDepositors = state.updatedThirdPartyDepositSettings?.depositorsAllowList?.filter {
                it != depositor
            }.orEmpty()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositorsAllowList = updatedDepositors
                ),
                deleteDialogState = DeleteDialogState.None
            )
        }
        checkIfSettingsChanged()
    }

    fun onAddAssetException() {
        _state.update { state ->
            val updatedAssetExceptions = (
                state.updatedThirdPartyDepositSettings?.assetsExceptionList.orEmpty() + listOf(
                    state.assetExceptionToAdd.assetException
                )
                ).distinct()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updatedAssetExceptions
                ),
                assetExceptionToAdd = AssetType.AssetException()
            )
        }
        checkIfSettingsChanged()
    }

    fun onAddDepositor() {
        _state.update { state ->
            when (Address(state.depositorToAdd.address).entityType()) {
                EntityType.GLOBAL_FUNGIBLE_RESOURCE_MANAGER -> ThirdPartyDeposits.DepositorAddress.ResourceAddress(
                    state.depositorToAdd.address
                )

                EntityType.GLOBAL_NON_FUNGIBLE_RESOURCE_MANAGER -> ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID(
                    state.depositorToAdd.address
                )

                else -> null
            }?.let { depositorAddress ->
                val updatedDepositors =
                    (state.updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty() + listOf(depositorAddress)).distinct()
                state.copy(
                    updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                        depositorsAllowList = updatedDepositors
                    ),
                    depositorToAdd = AssetType.Depositor()
                )
            } ?: state
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

    fun showDeletePrompt(depositor: ThirdPartyDeposits.DepositorAddress) {
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
            val updatedAssetExceptions = state.updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
                it != asset
            }.orEmpty()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updatedAssetExceptions
                ),
                deleteDialogState = DeleteDialogState.None
            )
        }
        checkIfSettingsChanged()
    }

    fun assetExceptionAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val valid = AddressValidator.isValid(
            address = address,
            networkId = currentNetworkId,
            allowedEntityTypes = setOf(EntityType.GLOBAL_FUNGIBLE_RESOURCE_MANAGER, EntityType.GLOBAL_NON_FUNGIBLE_RESOURCE_MANAGER)
        )

        _state.update { state ->
            val updatedException = state.assetExceptionToAdd.copy(
                assetException = state.assetExceptionToAdd.assetException.copy(address = address),
                addressValid = valid
            )
            state.copy(assetExceptionToAdd = updatedException)
        }
        checkIfSettingsChanged()
    }

    fun depositorAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val valid = AddressValidator.isValid(
            address = address,
            networkId = currentNetworkId,
            allowedEntityTypes = setOf(EntityType.GLOBAL_FUNGIBLE_RESOURCE_MANAGER, EntityType.GLOBAL_NON_FUNGIBLE_RESOURCE_MANAGER)
        )
        _state.update { state ->
            val updatedDepositor = state.depositorToAdd.copy(
                address = address,
                addressValid = valid
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
                    _state.update {
                        it.copy(
                            account = account,
                            updatedThirdPartyDepositSettings = account.onLedgerSettings.thirdPartyDeposits
                        )
                    }
                    checkIfSettingsChanged()
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
        val addressValid: Boolean = false
    )

    data class Depositor(val address: String = "", val addressValid: Boolean = false)
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
    val depositorToAdd: AssetType.Depositor = AssetType.Depositor("")
) : UiState {
    val allowedAssets: ImmutableList<ThirdPartyDeposits.AssetException>
        get() = updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
            it.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        }.orEmpty().toPersistentList()

    val deniedAssets: ImmutableList<ThirdPartyDeposits.AssetException>
        get() = updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
            it.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Deny
        }.orEmpty().toPersistentList()

    val allowedDepositors: ImmutableList<ThirdPartyDeposits.DepositorAddress>
        get() = updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty().toPersistentList()
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
