package com.babylon.wallet.android.presentation.settings.appsettings.backup

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.backup.BackupProfileToFileUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val changeBackupSettingUseCase: ChangeBackupSettingUseCase,
    private val backupProfileToFileUseCase: BackupProfileToFileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
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

        if (!isEncrypted) {
            viewModelScope.launch { sendEvent(Event.ChooseExportFile(State.FILE_NAME_NON_ENCRYPTED)) }
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
        _state.update {
            it.copy(encryptSheet = encryptSheet.copy(isPasswordRevealed = !encryptSheet.isPasswordRevealed))
        }
    }

    fun onEncryptConfirmPasswordTyped(password: String) {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update { it.copy(encryptSheet = encryptSheet.copy(confirm = password)) }
    }

    fun onEncryptConfirmPasswordRevealChange() {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        _state.update {
            it.copy(encryptSheet = encryptSheet.copy(isConfirmPasswordRevealed = !encryptSheet.isConfirmPasswordRevealed))
        }
    }

    fun onEncryptSubmitClick() {
        val encryptSheet = state.value.encryptSheet as? State.EncryptSheet.Open ?: return
        if (encryptSheet.isSubmitEnabled) {
            viewModelScope.launch { sendEvent(Event.ChooseExportFile(State.FILE_NAME_ENCRYPTED)) }
        }
    }

    fun onFileChosen(uri: Uri, deviceBiometricAuthenticationProvider: suspend () -> Boolean) = viewModelScope.launch {
        val fileBackupType = when (val sheet = state.value.encryptSheet) {
            is State.EncryptSheet.Closed -> BackupType.File.PlainText
            is State.EncryptSheet.Open -> BackupType.File.Encrypted(sheet.password)
        }
        if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist().not()) {
            val authenticationResult = deviceBiometricAuthenticationProvider()
            if (authenticationResult) {
                ensureBabylonFactorSourceExistUseCase()
            } else {
                Timber.w("Trying to back up profile without Babylon FS, should not happen!")
                backupProfileToFileUseCase.deleteFile(uri)
                // don't backup without Babylon Factor source!
                return@launch
            }
        }
        backupProfileToFileUseCase(fileBackupType = fileBackupType, file = uri).onSuccess {
            _state.update { state ->
                state.copy(uiMessage = UiMessage.InfoMessage.WalletExported, encryptSheet = State.EncryptSheet.Closed)
            }
        }.onFailure { error ->
            _state.update {
                it.copy(uiMessage = UiMessage.ErrorMessage.from(error), encryptSheet = State.EncryptSheet.Closed)
            }
        }
    }

    fun onDeleteWalletClick() {
        _state.update { it.copy(deleteWalletDialogVisible = true) }
    }

    fun onDeleteWalletConfirm() {
        _state.update { it.copy(deleteWalletDialogVisible = false) }

        viewModelScope.launch {
            deleteProfileUseCase()
            peerdroidClient.terminate()
            sendEvent(Event.ProfileDeleted)
        }
    }

    fun onDeleteWalletDeny() {
        _state.update { it.copy(deleteWalletDialogVisible = false) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val backupState: BackupState,
        val isExportFileDialogVisible: Boolean = false,
        val encryptSheet: EncryptSheet = EncryptSheet.Closed,
        val deleteWalletDialogVisible: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isBackupEnabled: Boolean
            get() = backupState is BackupState.Open

        val isEncryptSheetVisible: Boolean
            get() = encryptSheet is EncryptSheet.Open

        sealed interface EncryptSheet {
            object Closed : EncryptSheet
            data class Open(
                val password: String = "",
                val isPasswordRevealed: Boolean = false,
                val confirm: String = "",
                val isConfirmPasswordRevealed: Boolean = false,
            ) : EncryptSheet {

                val passwordsMatch: Boolean
                    get() = password == confirm

                val isSubmitEnabled: Boolean
                    get() = password.isNotBlank() && passwordsMatch
            }
        }

        companion object {
            const val FILE_NAME_NON_ENCRYPTED = "radix_wallet_backup_file.plaintext.json"
            const val FILE_NAME_ENCRYPTED = "radix_wallet_backup_file.encrypted.json"
        }
    }

    sealed interface Event : OneOffEvent {
        object Dismiss : Event
        object ProfileDeleted : Event
        data class ChooseExportFile(val fileName: String) : Event
    }
}
