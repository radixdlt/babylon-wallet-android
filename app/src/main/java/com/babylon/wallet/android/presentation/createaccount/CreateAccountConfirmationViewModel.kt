package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.truncatedHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<CreateAccountConfirmationEvent> by OneOffEventHandlerImpl() {

    internal val args = CreateAccountConfirmationArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            val account = accountRepository.getAccountByAddress(args.accountId)
            requireNotNull(account) {
                "account is null"
            }
            accountUiState = accountUiState.copy(
                accountName = account.displayName,
                accountAddressTruncated = account.address.truncatedHash(),
                appearanceId = account.appearanceID
            )
        }
    }

    var accountUiState by mutableStateOf(AccountConfirmationUiState())
        private set

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
        val accountAddressTruncated: String = "",
        val appearanceId: Int = 0,
    )
}

internal sealed interface CreateAccountConfirmationEvent : OneOffEvent {
    object NavigateToHome : CreateAccountConfirmationEvent
    object FinishAccountCreation : CreateAccountConfirmationEvent
}
