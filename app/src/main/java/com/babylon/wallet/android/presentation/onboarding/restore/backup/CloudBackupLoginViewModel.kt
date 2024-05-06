package com.babylon.wallet.android.presentation.onboarding.restore.backup

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class CloudBackupLoginViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<CloudBackupLoginViewModel.State>(),
    OneOffEventHandler<CloudBackupLoginViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onLoginToGoogleClick() = viewModelScope.launch {
        if (googleSignInManager.isCloudBackupAuthorized) {
            googleSignInManager.signOut()
            googleSignInManager.revokeAccess()
        }

        val intent = googleSignInManager.createSignInIntent()
        sendEvent(Event.SignInToGoogle(intent))
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            googleSignInManager.handleSignInResult(result)
                .onSuccess {
                    Timber.d("cloud backup is authorized")
                    sendEvent(Event.ProceedToRestoreFromBackup)
                }
                .onFailure { exception ->
                    if (exception is UserRecoverableAuthIOException) {
                        Timber.e("cloud backup authorization has been revoked, try to recover")
                        sendEvent(Event.RecoverUserAuthToDrive(exception.intent))
                    } else {
                        Timber.e("cloud backup authorization failed: $exception")
                        if (exception !is CancellationException) {
                            _state.update { state ->
                                state.copy(errorMessage = UiMessage.GoogleAuthErrorMessage(exception))
                            }
                        }
                    }
                }
        }
    }

    fun handleAuthDriveResult(result: ActivityResult) {
        viewModelScope.launch {
            val email = googleSignInManager.getSignedInGoogleAccount()?.email
            if (result.resultCode == Activity.RESULT_OK && email != null) {
                Timber.d("cloud backup is authorized")
                sendEvent(Event.ProceedToRestoreFromBackup)
            } else {
                Timber.e("cloud backup authorization failed")
                _state.update { state ->
                    state.copy(
                        errorMessage = UiMessage.GoogleAuthErrorMessage(
                            IOException("Failed with result code: ${result.resultCode}")
                        )
                    )
                }
            }
        }
    }

    // sign out and revoke access if previously the user authenticated/authorized Drive access
    // so the next screen RestoreFromBackupScreen won't show any available profiles, but the sign in button
    fun onSkipClick() = viewModelScope.launch {
        if (googleSignInManager.isCloudBackupAuthorized) {
            googleSignInManager.signOut()
            googleSignInManager.revokeAccess()
        }
        sendEvent(Event.ProceedToRestoreFromBackup)
    }

    fun onErrorMessageShown() = _state.update { it.copy(errorMessage = null) }

    data class State(
        val errorMessage: UiMessage.GoogleAuthErrorMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data class SignInToGoogle(val signInIntent: Intent) : Event
        data class RecoverUserAuthToDrive(val authIntent: Intent) : Event
        data object ProceedToRestoreFromBackup : Event
    }
}
