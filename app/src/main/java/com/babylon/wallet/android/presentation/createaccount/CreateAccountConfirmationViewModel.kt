package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<CreateAccountConfirmationEvent> by OneOffEventHandlerImpl() {

    private val hasProfile = savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE) ?: false
    private val accountName = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME).orEmpty()
    private val accountId = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID).orEmpty()

    var accountUiState by mutableStateOf(
        AccountConfirmationUiState(
            accountName = accountName,
            accountId = accountId
        )
    )
        private set

    fun goHomeClick() {
        viewModelScope.launch {
            if (hasProfile) {
                sendEvent(CreateAccountConfirmationEvent.FinishAccountCreation)
            } else {
                sendEvent(CreateAccountConfirmationEvent.NavigateToHome)
            }
        }
    }

    data class AccountConfirmationUiState(
        val accountName: String = "",
        val accountId: String = "",
    )
}

internal sealed interface CreateAccountConfirmationEvent : OneOffEvent {
    object NavigateToHome : CreateAccountConfirmationEvent
    object FinishAccountCreation : CreateAccountConfirmationEvent
}
