package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.viewModelScope
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
) : StateViewModel<BackupViewModel.State>() {

    override fun initialState(): State = State(backupState = BackupState.Closed)

    init {
        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                _state.update { it.copy(backupState = backupState) }
            }
        }
    }

    fun onBackupSettingChanged(isChecked: Boolean) = viewModelScope.launch {
        changeBackupSettingUseCase(isChecked)
    }

    data class State(
        val backupState: BackupState
    ) : UiState
}
