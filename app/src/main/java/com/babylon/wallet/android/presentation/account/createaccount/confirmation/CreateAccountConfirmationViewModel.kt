package com.babylon.wallet.android.presentation.account.createaccount.confirmation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
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
            val account = getProfileUseCase().activeAccountOnCurrentNetwork(args.accountId)
            requireNotNull(account) {
                "account is null"
            }
            _state.update {
                AccountConfirmationUiState(
                    accountName = account.displayName.value,
                    accountAddress = account.address.string,
                    appearanceId = account.appearanceId
                )
            }
        }
    }

    fun accountConfirmed() {
        viewModelScope.launch {
            when {
                args.requestSource.isFirstTime() -> sendEvent(CreateAccountConfirmationEvent.NavigateToHome)
                else -> sendEvent(CreateAccountConfirmationEvent.FinishAccountCreation)
            }
        }
    }

    data class AccountConfirmationUiState(
        val accountName: String = "",
        val accountAddress: String = "",
        val appearanceId: AppearanceId = AppearanceId(0u),
    ) : UiState
}

internal sealed interface CreateAccountConfirmationEvent : OneOffEvent {
    data object NavigateToHome : CreateAccountConfirmationEvent
    data object FinishAccountCreation : CreateAccountConfirmationEvent
}
