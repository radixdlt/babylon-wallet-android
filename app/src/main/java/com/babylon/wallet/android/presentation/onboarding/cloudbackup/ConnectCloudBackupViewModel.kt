package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ConnectCloudBackupViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<ConnectCloudBackupViewModel.State>(),
    OneOffEventHandler<ConnectCloudBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onLoginToGoogleClick() = viewModelScope.launch {
        _state.update { it.copy(isAccessToGoogleDriveInProgress = true) }

        val intent = googleSignInManager.createSignInIntent()
        sendEvent(Event.SignInToGoogle(intent))
    }

    fun onErrorMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _state.update { it.copy(isAccessToGoogleDriveInProgress = true) }

            googleSignInManager.handleSignInResult(result)
                .onSuccess { googleAccount ->
                    Timber.d("cloud backup is authorized for email: ${googleAccount.email}")
                    sendEvent(Event.ProceedToCreateAccountWithCloudBackupEnabled)
                }
                .onFailure { exception ->
                    if (exception is CancellationException) {
                        Timber.e("user cancelled sign in")
                    } else {
                        _state.update { state ->
                            state.copy(errorMessage = UiMessage.GoogleAuthErrorMessage(exception))
                        }
                        Timber.e("cloud backup authorization failed: $exception")
                    }
                }
                .also {
                    _state.update { it.copy(isAccessToGoogleDriveInProgress = false) }
                }
        }
    }

    data class State(
        val isAccessToGoogleDriveInProgress: Boolean = false,
        val errorMessage: UiMessage.GoogleAuthErrorMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {

        data class SignInToGoogle(val signInIntent: Intent) : Event

        data object ProceedToCreateAccountWithCloudBackupEnabled : Event
    }
}
