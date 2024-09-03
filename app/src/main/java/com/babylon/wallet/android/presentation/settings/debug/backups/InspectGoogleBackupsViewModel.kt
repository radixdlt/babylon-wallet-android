package com.babylon.wallet.android.presentation.settings.debug.backups

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.FetchBackedUpProfilesMetadataFromCloud
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InspectGoogleBackupsViewModel @Inject constructor(
    private val fetchBackedUpProfilesMetadataFromCloud: FetchBackedUpProfilesMetadataFromCloud,
    private val googleSignInManager: GoogleSignInManager,
    private val hostInfoRepository: HostInfoRepository
) : StateViewModel<InspectGoogleBackupsViewModel.State>() {

    override fun initialState(): State = State(isLoading = true)

    init {
        viewModelScope.launch {
            val email = googleSignInManager.getSignedInGoogleAccount()?.email
            val hostId = hostInfoRepository.getHostId().getOrNull()
            _state.update { it.copy(accountEmail = email, deviceId = hostId?.id) }

            if (email != null) {
                fetchFiles()
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onRefresh() = viewModelScope.launch {
        fetchFiles()
    }

    private suspend fun fetchFiles() {
        _state.update { it.copy(isLoading = true) }
        fetchBackedUpProfilesMetadataFromCloud()
            .onSuccess { files ->
                _state.update { it.copy(files = files, isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error), isLoading = false) }
            }
    }

    data class State(
        val isLoading: Boolean,
        val accountEmail: String? = null,
        val deviceId: UUID? = null,
        val files: List<CloudBackupFileEntity> = emptyList(),
        val uiMessage: UiMessage? = null
    ) : UiState
}
