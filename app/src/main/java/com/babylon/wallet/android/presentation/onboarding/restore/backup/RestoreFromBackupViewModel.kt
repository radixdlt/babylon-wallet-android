package com.babylon.wallet.android.presentation.onboarding.restore.backup

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Profile
import rdx.works.profile.domain.InvalidPasswordException
import rdx.works.profile.domain.InvalidSnapshotException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase
import javax.inject.Inject

@HiltViewModel
class RestoreFromBackupViewModel @Inject constructor(
    getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase
) : StateViewModel<RestoreFromBackupViewModel.State>(), OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToRestore = getTemporaryRestoringProfileForBackupUseCase(BackupType.Cloud)
            _state.update { it.copy(restoringProfile = profileToRestore) }
        }
    }

    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfile?.header?.isCompatible == true) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onRestoreFromFile(uri: Uri) = viewModelScope.launch {
        saveTemporaryRestoringSnapshotUseCase.forFile(uri = uri, BackupType.File.PlainText)
            .onSuccess {
                sendEvent(Event.OnRestoreConfirm(fromCloud = false))
            }.onFailure { error ->
                when (error) {
                    is InvalidPasswordException -> _state.update {
                        it.copy(passwordSheetState = State.PasswordSheet.Open(file = uri))
                    }
                    is InvalidSnapshotException -> _state.update {
                        it.copy(uiMessage = UiMessage.InfoMessage.InvalidSnapshot)
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

    fun onSubmitClick() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            sendEvent(Event.OnRestoreConfirm(fromCloud = true))
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
                        delay(AppConstants.DELAY_300_MS)
                        sendEvent(Event.OnRestoreConfirm(fromCloud = false))
                    }.onFailure { error ->
                        when (error) {
                            is InvalidPasswordException -> _state.update {
                                it.copy(passwordSheetState = sheet.copy(isPasswordInvalid = true))
                            }
                            is InvalidSnapshotException -> _state.update {
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

    data class State(
        val restoringProfile: Profile? = null,
        val isRestoringProfileChecked: Boolean = false,
        val passwordSheetState: PasswordSheet = PasswordSheet.Closed,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isPasswordSheetVisible: Boolean
            get() = passwordSheetState is PasswordSheet.Open

        val isContinueEnabled: Boolean
            get() = isRestoringProfileChecked

        sealed interface PasswordSheet {
            object Closed : PasswordSheet
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
        object OnDismiss : Event
        data class OnRestoreConfirm(val fromCloud: Boolean) : Event
    }
}
