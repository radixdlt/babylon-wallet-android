package com.babylon.wallet.android.presentation.account.settings.delete.moveassets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.usecases.PrepareTransactionForAccountDeletionUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeletingAccountMoveAssetsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository,
    private val prepareTransactionForAccountDeletionUseCase: PrepareTransactionForAccountDeletionUseCase,
    private val savedStateHandle: SavedStateHandle
) : StateViewModel<DeletingAccountMoveAssetsViewModel.State>() {

    override fun initialState(): State = State(
        deletingAccountAddress = DeletingAccountMoveAssetsArgs(savedStateHandle).deletingAccountAddress
    )

    init {
        viewModelScope.launch {
            val accounts = getProfileUseCase().activeAccountsOnCurrentNetwork.filterNot { it.address == state.value.deletingAccountAddress }

            stateRepository.getOwnedXRD(accounts).onSuccess { accountsWithXrd ->
                _state.update {
                    it.copy(
                        accountsWithBalances = accountsWithXrd,
                        isFetchingBalances = false
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        isFetchingBalances = false
                    )
                }
            }
        }
    }

    fun onSkipRequested() {
        _state.update { it.copy(warning = State.Warning.SkipMovingAssets) }
    }

    fun onSkipConfirmed() {
        _state.update {
            it.copy(warning = null, isPreparingManifest = true, isSkipSelected = true)
        }

        viewModelScope.launch { prepareTransaction() }
    }

    fun onSkipCancelled() {
        _state.update { it.copy(warning = null) }
    }

    fun onAccountSelected(account: Account) {
        if (!state.value.isAccountEnabled(account)) return

        _state.update { it.copy(selectedAccount = account) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onSubmit() {
        _state.update { it.copy(isPreparingManifest = true, isSkipSelected = false) }

        viewModelScope.launch { prepareTransaction() }
    }

    private suspend fun prepareTransaction() {
        prepareTransactionForAccountDeletionUseCase(
            deletingAccountAddress = state.value.deletingAccountAddress,
            accountAddressToTransferResources = if (state.value.isSkipSelected) null else state.value.selectedAccount?.address
        ).onSuccess {
            _state.update { state -> state.copy(isPreparingManifest = false, isSkipSelected = false) }
        }.onFailure { error ->
            Timber.w(error.message)
            val warning = when (error) {
                is CommonException.MaxTransfersPerTransactionReached -> State.Warning.CannotDeleteAccount
                // TODO handle the error for not transferable assets
                else -> null
            }

            _state.update {
                it.copy(
                    isPreparingManifest = false,
                    isSkipSelected = false,
                    warning = warning,
                    uiMessage = UiMessage.ErrorMessage(error).takeIf { warning == null }
                )
            }
        }
    }

    data class State(
        val deletingAccountAddress: AccountAddress,
        val selectedAccount: Account? = null,
        val warning: Warning? = null,
        val uiMessage: UiMessage? = null,
        val isSkipSelected: Boolean = false,
        private val isFetchingBalances: Boolean = true,
        private val isPreparingManifest: Boolean = false,
        private val accountsWithBalances: Map<Account, Decimal192> = emptyMap()
    ) : UiState {

        val isAccountsLoading: Boolean
            get() = isFetchingBalances

        val isContinueLoading: Boolean
            get() = isPreparingManifest && !isSkipSelected

        val isSkipLoading: Boolean
            get() = isPreparingManifest && isSkipSelected

        fun accounts(): List<Account> = accountsWithBalances.keys.toList()

        fun isAccountSelected(account: Account): Boolean = selectedAccount == account

        fun isAccountEnabled(account: Account): Boolean = isEnoughXRD(account)

        fun isNotAnyAccountsWithEnoughXRDWarningVisible(): Boolean =
            !isFetchingBalances && (accountsWithBalances.isEmpty() || accountsWithBalances.none { isEnoughXRD(it.key) })

        private fun isEnoughXRD(account: Account): Boolean = accountsWithBalances.getOrDefault(account, zero) > xrdThreshold

        enum class Warning {
            SkipMovingAssets,
            CannotDeleteAccount,
            CannotTransferSomeAssets
        }

        companion object {
            private val zero = 0.toDecimal192()
            private val xrdThreshold = 2.toDecimal192()
        }
    }
}
