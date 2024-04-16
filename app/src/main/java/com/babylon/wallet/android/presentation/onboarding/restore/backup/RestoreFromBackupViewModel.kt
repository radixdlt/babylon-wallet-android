package com.babylon.wallet.android.presentation.onboarding.restore.backup

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
import com.radixdlt.sargon.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.core.sargon.isCompatible
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RestoreFromBackupViewModel @Inject constructor(
    getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase,
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<RestoreFromBackupViewModel.State>(),
    OneOffEventHandler<RestoreFromBackupViewModel.RestoreFromBackupEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToRestore = getTemporaryRestoringProfileForBackupUseCase(BackupType.Cloud)
            _state.update { it.copy(restoringProfile = profileToRestore) }
        }
    }

    @Deprecated("Remove when new cloud back up system in place")
    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfile?.header?.isCompatible == true) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onRestoreFromFile(uri: Uri) = viewModelScope.launch {
        saveTemporaryRestoringSnapshotUseCase.forFile(uri = uri, BackupType.File.PlainText)
            .onSuccess {
                sendEvent(RestoreFromBackupEvent.OnRestoreConfirm(fromCloud = false))
            }.onFailure { error ->
                when (error) {
                    is ProfileException.InvalidPassword -> _state.update {
                        it.copy(passwordSheetState = State.PasswordSheet.Open(file = uri))
                    }

                    is ProfileException.InvalidSnapshot -> _state.update {
                        it.copy(uiMessage = UiMessage.InfoMessage.InvalidSnapshot)
                    }
                }
            }
    }

    fun onBackClick() {
        if (!state.value.isPasswordSheetVisible) {
            viewModelScope.launch { sendEvent(RestoreFromBackupEvent.OnDismiss) }
        } else {
            _state.update { it.copy(passwordSheetState = State.PasswordSheet.Closed) }
        }
    }

    fun onSubmitClick() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            sendEvent(RestoreFromBackupEvent.OnRestoreConfirm(fromCloud = true))
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
                        sendEvent(RestoreFromBackupEvent.OnRestoreConfirm(fromCloud = false))
                    }.onFailure { error ->
                        when (error) {
                            is ProfileException.InvalidPassword -> _state.update {
                                it.copy(passwordSheetState = sheet.copy(isPasswordInvalid = true))
                            }

                            is ProfileException.InvalidSnapshot -> _state.update {
                                it.copy(
                                    passwordSheetState = State.PasswordSheet.Closed,
                                    uiMessage = UiMessage.InfoMessage.InvalidSnapshot
                                )
                            }
                        }
                    }
            }
        }
    }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    fun turnOnCloudBackup() = viewModelScope.launch {
        val intent = googleSignInManager.createSignInIntent()
        sendEvent(RestoreFromBackupEvent.SignInToGoogle(intent))
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            googleSignInManager.handleSignInResult(result)
                .onSuccess { googleAccount ->
                    _state.update { it.copy(backupEmail = googleAccount.email) }
                    Timber.d("cloud backup is authorized")
                }
                .onFailure { exception ->
                    _state.update { state -> state.copy(backupEmail = "") }
                    Timber.e("cloud backup authorization failed: ${exception.message}")
                }
        }
    }

    data class State(
        val restoringProfile: Profile? = null,
        val isRestoringProfileChecked: Boolean = false,
        val passwordSheetState: PasswordSheet = PasswordSheet.Closed,
        val backupEmail: String = "",
        val uiMessage: UiMessage? = null
    ) : UiState {

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

    sealed interface RestoreFromBackupEvent : OneOffEvent {
        data object OnDismiss : RestoreFromBackupEvent
        data class OnRestoreConfirm(val fromCloud: Boolean) : RestoreFromBackupEvent
        data class SignInToGoogle(val signInIntent: Intent) : RestoreFromBackupEvent
    }
}
