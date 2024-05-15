package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.CanSignInToGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.cloudbackup.model.GoogleAccount
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ConnectCloudBackupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<ConnectCloudBackupViewModel.State>(),
    CanSignInToGoogle,
    OneOffEventHandler<ConnectCloudBackupViewModel.Event> by OneOffEventHandlerImpl() {

    private val args: ConnectCloudBackupArgs = ConnectCloudBackupArgs(savedStateHandle)

    override fun initialState(): State = State(mode = args.mode)

    override fun signInManager(): GoogleSignInManager = googleSignInManager

    override fun onSignInResult(result: Result<GoogleAccount>) {
        viewModelScope.launch {
            result.onSuccess { googleAccount ->
                Timber.tag("CloudBackup").d("Authorized for email: ${googleAccount.email}")
                sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = true))
            }.onFailure { exception ->
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
    }

    fun onLoginToGoogleClick() = viewModelScope.launch {
        _state.update { it.copy(isConnecting = true) }

        if (googleSignInManager.isSignedIn()) {
            googleSignInManager.signOut()
        }

        sendEvent(Event.SignInToGoogle)
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

        data object SignInToGoogle : Event
        data class Proceed(val mode: ConnectMode, val isCloudBackupEnabled: Boolean): Event
    }
}
