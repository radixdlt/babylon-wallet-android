package com.babylon.wallet.android.presentation.settings.debug.backups

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.google.api.services.drive.model.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.FetchBackedUpProfilesMetadataFromCloud
import javax.inject.Inject

@HiltViewModel
class InspectGoogleBackupsViewModel @Inject constructor(
    private val fetchBackedUpProfilesMetadataFromCloud: FetchBackedUpProfilesMetadataFromCloud,
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<InspectGoogleBackupsViewModel.State>() {


    override fun initialState(): State = State(isLoading = true)

    init {
        viewModelScope.launch {
            val email = googleSignInManager.getSignedInGoogleAccount()?.email
            _state.update { it.copy(accountEmail = email) }

            if (email != null) {
                delay(2000)
                fetchBackedUpProfilesMetadataFromCloud()
                    .onSuccess { files ->
                        _state.update { it.copy(files = files, isLoading = false) }
                    }.onFailure { error ->
                        _state.update { it.copy(uiMessage = UiMessage.GoogleAuthErrorMessage(error), isLoading = false) }
                    }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val isLoading: Boolean,
        val accountEmail: String? = null,
        val files: List<CloudBackupFileEntity> = emptyList(),
        val uiMessage: UiMessage? = null
    ) : UiState
}