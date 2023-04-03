package com.babylon.wallet.android.presentation.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<CreateAccountConfirmationEvent> by OneOffEventHandlerImpl() {

    internal val args = CreateAccountConfirmationArgs(savedStateHandle)
    private val _state = MutableStateFlow(AccountConfirmationUiState())
    val state: StateFlow<AccountConfirmationUiState> = _state

    init {
        viewModelScope.launch {
            val account = accountRepository.getAccountByAddress(args.accountId)
            requireNotNull(account) {
                "account is null"
            }
            _state.value = AccountConfirmationUiState(
                accountName = account.displayName,
                accountAddress = account.address,
                appearanceId = account.appearanceID
            )
        }
    }

    fun accountConfirmed() {
        viewModelScope.launch {
            when (args.requestSource) {
                CreateAccountRequestSource.FirstTime -> sendEvent(CreateAccountConfirmationEvent.NavigateToHome)
                else -> sendEvent(CreateAccountConfirmationEvent.FinishAccountCreation)
            }
        }
    }

    data class AccountConfirmationUiState(
        val accountName: String = "",
        val accountAddress: String = "",
        val appearanceId: Int = 0,
    )
}

internal sealed interface CreateAccountConfirmationEvent : OneOffEvent {
    object NavigateToHome : CreateAccountConfirmationEvent
    object FinishAccountCreation : CreateAccountConfirmationEvent
}
