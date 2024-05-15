package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.SavedStateHandle
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
class ConnectCloudBackupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<ConnectCloudBackupViewModel.State>(),
    OneOffEventHandler<ConnectCloudBackupViewModel.Event> by OneOffEventHandlerImpl() {

    private val args: ConnectCloudBackupArgs = ConnectCloudBackupArgs(savedStateHandle)

    override fun initialState(): State = State(mode = args.mode)

    fun onLoginToGoogleClick() = viewModelScope.launch {
        _state.update { it.copy(isConnecting = true) }

        if (googleSignInManager.isSignedIn()) {
            googleSignInManager.signOut()
        }

        sendEvent(Event.SignInToGoogle(googleSignInManager.createSignInIntent()))
    }

    fun onErrorMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onSkipClick() = viewModelScope.launch {
        if (googleSignInManager.isSignedIn()) {
            googleSignInManager.signOut()
        }

        sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = false))
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true) }

            googleSignInManager.handleSignInResult(result)
                .onSuccess { googleAccount ->
                    Timber.tag("CloudBackup").d("Authorized for email: ${googleAccount.email}")
                    sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = true))
                }
                .onFailure { exception ->
                    if (exception is UserRecoverableAuthIOException) {
                        Timber.tag("CloudBackup").e("cloud backup authorization has been revoked, try to recover")
                        sendEvent(Event.RecoverUserAuthToDrive(exception.intent))
                    } else {
                        if (exception is CancellationException) {
                            Timber.tag("CloudBackup").e("User cancelled sign in")
                        } else {
                            _state.update { state ->
                                state.copy(errorMessage = UiMessage.GoogleAuthErrorMessage(exception))
                            }
                            Timber.tag("CloudBackup").e("Authorization failed: $exception")
                        }
                    }
                }
                .also {
                    _state.update { state ->
                        state.copy(isConnecting = false)
                    }
                }
        }
    }

    fun handleAuthDriveResult(result: ActivityResult) {
        viewModelScope.launch {
            val email = googleSignInManager.getSignedInGoogleAccount()?.email
            if (result.resultCode == Activity.RESULT_OK && email != null) {
                Timber.d("cloud backup is authorized")
                sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = true))
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

    data class State(
        val mode: ConnectMode,
        val isConnecting: Boolean = false,
        val errorMessage: UiMessage.GoogleAuthErrorMessage? = null
    ) : UiState

    enum class ConnectMode {
        NewWallet,
        RestoreWallet
    }

    sealed interface Event : OneOffEvent {

        data class SignInToGoogle(val signInIntent: Intent) : Event
        data class RecoverUserAuthToDrive(val authIntent: Intent) : Event
        data class Proceed(val mode: ConnectMode, val isCloudBackupEnabled: Boolean): Event
    }
}
