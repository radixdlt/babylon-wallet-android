package com.babylon.wallet.android.presentation.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.utils.OneOffEventHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hasProfile = savedStateHandle.get<Boolean>(Screen.ARG_HAS_PROFILE) ?: false
    private val accountName = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_NAME).orEmpty()
    private val accountId = savedStateHandle.get<String>(Screen.ARG_ACCOUNT_ID).orEmpty()

    private val _composeEvent = OneOffEventHandler<ComposeEvent>()
    val composeEvent by _composeEvent

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
                _composeEvent.sendEvent(ComposeEvent.FinishAccountCreation)
            } else {
                _composeEvent.sendEvent(ComposeEvent.NavigateToHome)
            }
        }
    }

    data class AccountConfirmationUiState(
        val accountName: String = "",
        val accountId: String = ""
    )

    sealed interface ComposeEvent {
        object NavigateToHome : ComposeEvent
        object FinishAccountCreation : ComposeEvent
    }
}
