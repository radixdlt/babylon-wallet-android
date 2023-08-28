package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.Address
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber
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
        prepareThirdPartyDepositUpdateRequest(AccountDefaultDepositRule.ACCEPT)
    }

    private fun prepareThirdPartyDepositUpdateRequest(rule: AccountDefaultDepositRule) {
        viewModelScope.launch {
            val networkId = requireNotNull(state.value.account?.networkID)
            BabylonManifestBuilder().setDefaultDepositRule(
                accountAddress = Address(args.address),
                accountDefaultDepositRule = rule
            ).buildSafely(networkId).onSuccess { manifest ->
                Timber.d("Approving: \n${manifest.toPrettyString()}")
                incomingRequestRepository.add(
                    manifest.prepareInternalTransactionRequest(networkId)
                )
            }.onFailure { t ->
                _state.update { state ->
                    state.copy(error = UiMessage.ErrorMessage.from(t))
                }
            }
        }
    }

    fun onDenyAll() {
        prepareThirdPartyDepositUpdateRequest(AccountDefaultDepositRule.REJECT)
    }

    fun onAcceptKnown() {
        prepareThirdPartyDepositUpdateRequest(AccountDefaultDepositRule.ALLOW_EXISTING)
    }

    private fun loadAccount() {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.map { accounts -> accounts.first { it.address == args.address } }
                .collect { account ->
                    _state.update {
                        it.copy(
                            account = account,
                            accountDepositRule = account.onLedgerSettings.thirdPartyDeposits.depositRule
                        )
                    }
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }
}

data class AccountThirdPartyDepositsUiState(
    val account: Network.Account? = null,
    val accountAddress: String,
    val accountDepositRule: Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule? = null,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
) : UiState