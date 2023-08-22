package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val changeBackupSettingUseCase: ChangeBackupSettingUseCase,
    getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<BackupViewModel.State>(), OneOffEventHandler<BackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(backupState = BackupState.Closed)

    init {
        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                _state.update { it.copy(backupState = backupState) }
            }
        }
    }

    fun onBackClick() {
        if (state.value.isEncryptSheetVisible) {
            _state.update { it.copy(encryptSheet = State.EncryptSheet.Closed) }
        } else {
            viewModelScope.launch { sendEvent(Event.Dismiss) }
        }
    }

    fun onBackupSettingChanged(isChecked: Boolean) = viewModelScope.launch {
        changeBackupSettingUseCase(isChecked)
    }

    fun onFileBackupClick() {
        _state.update { it.copy(isExportFileDialogVisible = true) }
    }

    fun onFileBackupConfirm(isEncrypted: Boolean) {
        _state.update {
            it.copy(
                isExportFileDialogVisible = false,
                encryptSheet = if (isEncrypted) State.EncryptSheet.Open() else State.EncryptSheet.Closed
            )
        }
    }

    fun onFileBackupDeny() {
        _state.update { it.copy(isExportFileDialogVisible = false) }
    }

    fun onEncryptPasswordTyped(password: String) {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update { it.copy(encryptSheet = encryptSheet.copy(password = password)) }
    }

    fun onEncryptPasswordRevealChange() {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update { it.copy(encryptSheet = encryptSheet.copy(isPasswordRevealed = !encryptSheet.isPasswordRevealed)) }
    }

    fun onEncryptConfirmPasswordTyped(password: String) {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update { it.copy(encryptSheet = encryptSheet.copy(confirm = password)) }
    }

    fun onEncryptConfirmPasswordRevealChange() {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update { it.copy(encryptSheet = encryptSheet.copy(isConfirmPasswordRevealed = !encryptSheet.isConfirmPasswordRevealed)) }
    }

    fun onEncryptSubmitClick() {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
    }

    data class State(
        val backupState: BackupState,
        val isExportFileDialogVisible: Boolean = false,
        val encryptSheet: EncryptSheet = EncryptSheet.Closed
    ) : UiState {

        val isBackupEnabled: Boolean
            get() = backupState is BackupState.Open

        val isEncryptSheetVisible: Boolean
            get() = encryptSheet is EncryptSheet.Open

        sealed interface EncryptSheet {
            object Closed: EncryptSheet
            data class Open(
                val password: String = "",
                val isPasswordRevealed: Boolean = false,
                val confirm: String = "",
                val isConfirmPasswordRevealed: Boolean = false,
            ): EncryptSheet {

                val passwordsMatch: Boolean
                    get() = password == confirm

                val isSubmitEnabled: Boolean
                    get() = password.isNotBlank() && passwordsMatch
            }
        }
    }

    sealed interface Event: OneOffEvent {
        object Dismiss: Event
    }
}
