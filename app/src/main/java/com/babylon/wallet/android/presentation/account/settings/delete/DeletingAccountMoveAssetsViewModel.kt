package com.babylon.wallet.android.presentation.account.settings.delete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class DeletingAccountMoveAssetsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository,
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
        _state.update { it.copy(isSkipDialogVisible = true) }
    }

    fun onSkipConfirmed() {
        _state.update { it.copy(isSkipDialogVisible = false) }
    }

    fun onSkipCancelled() {
        _state.update { it.copy(isSkipDialogVisible = false) }
    }

    fun onAccountSelected(account: Account) {
        if (!state.value.isAccountEnabled(account)) return

        _state.update { it.copy(selectedAccount = account) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onSubmit() {
        _state.update { it.copy(isFetchingAssetsForDeletingAccount = true) }

        // TODO
        // 1. Request for manifest
        // 2. On Success show transaction
        // 3. Check error if resources too many
        // 4. Any other error
    }

    data class State(
        val deletingAccountAddress: AccountAddress,
        val isFetchingBalances: Boolean = true,
        val isFetchingAssetsForDeletingAccount: Boolean = false,
        val selectedAccount: Account? = null,
        val isSkipDialogVisible: Boolean = false,
        val isCannotDeleteAccountVisible: Boolean = false,
        private val accountsWithBalances: Map<Account, Decimal192> = emptyMap(),
        val uiMessage: UiMessage? = null
    ) : UiState {

        fun accounts(): List<Account> = accountsWithBalances.keys.toList()

        fun isAccountSelected(account: Account): Boolean = selectedAccount == account

        fun isAccountEnabled(account: Account): Boolean = isEnoughXRD(account)

        fun isNotAnyAccountsWithEnoughXRDWarningVisible(): Boolean =
            !isFetchingBalances && (accountsWithBalances.isEmpty() || accountsWithBalances.none { isEnoughXRD(it.key) })

        private fun isEnoughXRD(account: Account): Boolean = accountsWithBalances.getOrDefault(account, zero) > xrdThreshold

        companion object {
            private val zero = 0.toDecimal192()
            private val xrdThreshold = zero
        }
    }
}