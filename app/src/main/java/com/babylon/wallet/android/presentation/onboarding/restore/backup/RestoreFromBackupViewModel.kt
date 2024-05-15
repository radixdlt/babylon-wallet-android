package com.babylon.wallet.android.presentation.onboarding.restore.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.radixdlt.sargon.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.isCompatible
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
@Suppress("TooManyFunctions")
class RestoreFromBackupViewModel @Inject constructor(
    private val getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase,
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<RestoreFromBackupViewModel.State>(),
    OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        // if user has enabled cloud backup (from previous screen)
        // then restore profiles from backup
        googleSignInManager.getSignedInGoogleAccount()?.email?.let { email ->
            _state.update {
                it.copy(backupEmail = email)
            }
            viewModelScope.launch {
                restoreProfileFromCloudBackup()
            }
        }
    }

    @Deprecated("Remove when new cloud back up system in place")
    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfiles.first().header.isCompatible) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onRestoreFromFile(uri: Uri) = viewModelScope.launch {
        saveTemporaryRestoringSnapshotUseCase.forFile(uri = uri, BackupType.File.PlainText)
            .onSuccess {
                sendEvent(Event.OnRestoreConfirm(fromCloud = false))
            }.onFailure { error ->
                when (error) {
                    is ProfileException.InvalidPassword -> _state.update {
                        it.copy(passwordSheetState = State.PasswordSheet.Open(file = uri))
                    }

                    is ProfileException.InvalidSnapshot -> _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(error))
                    }
                }
            }
    }

    fun onLoginToGoogleClick() = viewModelScope.launch {
        val intent = googleSignInManager.createSignInIntent()
        sendEvent(Event.SignInToGoogle(intent))
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            googleSignInManager.handleSignInResult(result)
                .onSuccess { googleAccount ->
                    _state.update { it.copy(backupEmail = googleAccount.email) }
                    Timber.d("cloud backup is authorized")
                    restoreProfileFromCloudBackup()
                }
                .onFailure { exception ->
                    if (exception is UserRecoverableAuthIOException) {
                        Timber.e("cloud backup authorization has been revoked, try to recover")
                        sendEvent(Event.RecoverUserAuthToDrive(exception.intent))
                    } else {
                        Timber.e("cloud backup authorization failed: $exception")
                        if (exception !is CancellationException) {
                            _state.update { state ->
                                state.copy(
                                    backupEmail = "",
                                    uiMessage = UiMessage.GoogleAuthErrorMessage(exception)
                                )
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
                _state.update { it.copy(backupEmail = email) }
                restoreProfileFromCloudBackup()
            } else {
                Timber.e("cloud backup authorization failed: ${result.resultCode}")
                _state.update { state ->
                    state.copy(
                        backupEmail = "",
                        uiMessage = UiMessage.GoogleAuthErrorMessage(
                            IOException("Failed with result code: ${result.resultCode}")
                        )
                    )
                }
            }
        }
    }

    fun onPasswordTyped(password: String) {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        _state.update {
            it.copy(passwordSheetState = sheet.copy(password = password, isPasswordInvalid = false))
        }
    }

    fun onPasswordRevealToggle() {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        _state.update {
            it.copy(passwordSheetState = sheet.copy(isPasswordRevealed = !sheet.isPasswordRevealed))
        }
    }

    fun onPasswordSubmitted() {
        val sheet = state.value.passwordSheetState as? State.PasswordSheet.Open ?: return
        if (sheet.isSubmitEnabled) {
            viewModelScope.launch {
                saveTemporaryRestoringSnapshotUseCase.forFile(
                    uri = sheet.file,
                    fileBackupType = BackupType.File.Encrypted(sheet.password)
                )
                    .onSuccess {
                        _state.update { state -> state.copy(passwordSheetState = State.PasswordSheet.Closed) }
                        delay(Constants.DELAY_300_MS)
                        sendEvent(Event.OnRestoreConfirm(fromCloud = false))
                    }.onFailure { error ->
                        when (error) {
                            is ProfileException.InvalidPassword -> _state.update {
                                it.copy(passwordSheetState = sheet.copy(isPasswordInvalid = true))
                            }

                            is ProfileException.InvalidSnapshot -> _state.update {
                                it.copy(
                                    passwordSheetState = State.PasswordSheet.Closed,
                                    uiMessage = UiMessage.ErrorMessage(ProfileException.InvalidSnapshot)
                                )
                            }
                        }
                    }
            }
        }
    }

    fun onBackClick() {
        if (!state.value.isPasswordSheetVisible) {
            viewModelScope.launch { sendEvent(Event.OnDismiss) }
        } else {
            _state.update { it.copy(passwordSheetState = State.PasswordSheet.Closed) }
        }
    }

    fun onContinueClick() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            sendEvent(Event.OnRestoreConfirm(fromCloud = true))
        }
    }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    private suspend fun restoreProfileFromCloudBackup() {
        getTemporaryRestoringProfileForBackupUseCase(BackupType.Cloud)?.let { restoringProfile ->
            if (restoringProfile.header.isCompatible) {
                _state.update {
                    it.copy(restoringProfiles = listOf(restoringProfile))
                }
            }
        }
    }

    data class State(
        private val backupEmail: String = "",
        val isRestoringProfileChecked: Boolean = false,
        val restoringProfiles: List<Profile> = persistentListOf(),
        val passwordSheetState: PasswordSheet = PasswordSheet.Closed,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isCloudBackupAuthorized: Boolean
            get() = backupEmail.isEmpty().not()

        val isPasswordSheetVisible: Boolean
            get() = passwordSheetState is PasswordSheet.Open

        val isContinueEnabled: Boolean
            get() = isRestoringProfileChecked

        sealed interface PasswordSheet {
            data object Closed : PasswordSheet
            data class Open(
                val password: String = "",
                val isPasswordInvalid: Boolean = false,
                val isPasswordRevealed: Boolean = false,
                val file: Uri
            ) : PasswordSheet {

                val isSubmitEnabled: Boolean
                    get() = password.isNotBlank() && !isPasswordInvalid
            }
        }
    }

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
        data class SignInToGoogle(val signInIntent: Intent) : Event
        data class RecoverUserAuthToDrive(val authIntent: Intent) : Event
        data class OnRestoreConfirm(val fromCloud: Boolean) : Event
    }
}
