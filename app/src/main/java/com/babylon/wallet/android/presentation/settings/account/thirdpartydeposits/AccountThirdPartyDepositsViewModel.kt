package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.ret.Address
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.AddressValidator
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.data.utils.toRETDepositRule
import rdx.works.profile.data.utils.toRETResourcePreference
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountThirdPartyDepositsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountThirdPartyDepositsUiState>() {

    private val args = AccountThirdPartyDepositsArgs(savedStateHandle)

    override fun initialState(): AccountThirdPartyDepositsUiState = AccountThirdPartyDepositsUiState(accountAddress = args.address)

    init {
        loadAccount()
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
            if (state.value.account?.onLedgerSettings?.thirdPartyDeposits?.depositRule != state.value.updatedThirdPartyDepositSettings?.depositRule) {
                val depositRule = checkNotNull(state.value.updatedThirdPartyDepositSettings?.depositRule?.toRETDepositRule())
                manifestBuilder.setDefaultDepositRule(
                    accountAddress = Address(args.address),
                    accountDefaultDepositRule = depositRule
                )
            }
            val currentAssetExceptions = state.value.account?.onLedgerSettings?.thirdPartyDeposits?.assetsExceptionList.orEmpty()
            val currentDepositors = state.value.account?.onLedgerSettings?.thirdPartyDeposits?.depositorsAllowList.orEmpty()
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
                manifestBuilder.removeAuthorizedDepositor(Address(args.address), Address(deletedDepositor.address))
            }
            newDepositors.minus(currentDepositors.toSet()).forEach { addedDepositor ->
                manifestBuilder.addAuthorizedDepositor(
                    accountAddress = Address(args.address),
                    depositorAddress = Address(addedDepositor.address),
                )
            }
            manifestBuilder.buildSafely(networkId).onSuccess { manifest ->
                val updatedThirdPartyDepositSettings = state.value.updatedThirdPartyDepositSettings ?: return@onSuccess
                incomingRequestRepository.add(
                    manifest.prepareInternalTransactionRequest(
                        networkId,
                        transactionType = TransactionType.UpdateThirdPartyDeposits(updatedThirdPartyDepositSettings),
                        blockUntilCompleted = true
                    )
                )
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
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun onAddAssetException() {
        _state.update { state ->
            val updatedAssetExceptions =
                (state.updatedThirdPartyDepositSettings?.assetsExceptionList.orEmpty() + listOf(
                    state.assetExceptionToAdd.assetException
                )).distinct()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updatedAssetExceptions
                ),
                assetExceptionToAdd = AssetType.Exception()
            )
        }
        checkIfSettingsChanged()
    }

    fun onAddDepositor() {
        _state.update { state ->
            val updatedDepositors =
                (state.updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty() + listOf(
                    ThirdPartyDeposits.DepositorAddress.ResourceAddress(state.depositorToAdd.address)
                )).distinct()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    depositorsAllowList = updatedDepositors
                ),
                depositorToAdd = AssetType.Depositor()
            )
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

    fun onDeleteAsset(asset: ThirdPartyDeposits.AssetException) {
        _state.update { state ->
            val updatedAssetExceptions = state.updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
                it != asset
            }.orEmpty()
            state.copy(
                updatedThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings?.copy(
                    assetsExceptionList = updatedAssetExceptions
                )
            )
        }
        checkIfSettingsChanged()
    }

    fun assetExceptionAddressTyped(address: String) {
        val currentNetworkId = state.value.account?.networkID ?: return
        val valid = AddressValidator.hasResourcePrefix(address) && AddressValidator.isValid(
            address = address,
            networkId = currentNetworkId
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
        val valid = AddressValidator.hasResourcePrefix(address) && AddressValidator.isValid(
            address = address,
            networkId = currentNetworkId
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
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }
}

sealed class AssetType {
    data class Exception(
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
    val error: UiMessage? = null,
    val assetExceptionToAdd: AssetType.Exception = AssetType.Exception(
        ThirdPartyDeposits.AssetException(
            "",
            ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        )
    ),
    val depositorToAdd: AssetType.Depositor = AssetType.Depositor("")
) : UiState {
    val allowedAssets: List<ThirdPartyDeposits.AssetException>
        get() = updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
            it.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Allow
        }.orEmpty()

    val deniedAssets: List<ThirdPartyDeposits.AssetException>
        get() = updatedThirdPartyDepositSettings?.assetsExceptionList?.filter {
            it.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Deny
        }.orEmpty()

    val allowedDepositors: List<ThirdPartyDeposits.DepositorAddress>
        get() = updatedThirdPartyDepositSettings?.depositorsAllowList.orEmpty()
}
