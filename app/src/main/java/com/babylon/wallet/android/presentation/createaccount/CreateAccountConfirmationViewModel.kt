package com.babylon.wallet.android.presentation.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<CreateAccountConfirmationViewModel.AccountConfirmationUiState>(),
    OneOffEventHandler<CreateAccountConfirmationEvent> by OneOffEventHandlerImpl() {

    internal val args = CreateAccountConfirmationArgs(savedStateHandle)

    override fun initialState(): AccountConfirmationUiState = AccountConfirmationUiState()

    init {
        viewModelScope.launch {
            val account = getProfileUseCase.accountOnCurrentNetwork(args.accountId)
            requireNotNull(account) {
                "account is null"
            }
            _state.update {
                AccountConfirmationUiState(
                    accountName = account.displayName,
                    accountAddress = account.address,
                    appearanceId = account.appearanceID
                )
            }
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
    ) : UiState
}

internal sealed interface CreateAccountConfirmationEvent : OneOffEvent {
    object NavigateToHome : CreateAccountConfirmationEvent
    object FinishAccountCreation : CreateAccountConfirmationEvent
}
