package com.babylon.wallet.android.presentation.claimed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.DriveClient
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ClaimedByAnotherDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cloudBackupErrorStream: CloudBackupErrorStream,
    private val profileRepository: ProfileRepository,
    private val driveClient: DriveClient
) : StateViewModel<ClaimedByAnotherDeviceViewModel.State>(),
    OneOffEventHandler<ClaimedByAnotherDeviceViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ClaimedByAnotherDeviceArgs(savedStateHandle)

    override fun initialState(): State = State()

    fun onResetWallet() = viewModelScope.launch {
        profileRepository.clearAllWalletData()
        cloudBackupErrorStream.resetErrors()
        sendEvent(Event.ResetToOnboarding)
    }

    fun onReclaim() = viewModelScope.launch {
        _state.update { it.copy(isReclaiming = true) }

        driveClient.claimCloudBackup(
            file = args.claimedEntity,
            profileModifiedTime = args.modifiedTime
        ).onSuccess {
            cloudBackupErrorStream.resetErrors()
            sendEvent(Event.Dismiss)
        }.onFailure {
            // TODO check error
            _state.update { state -> state.copy(isReclaiming = false) }
        }
    }

    data class State(
        val isReclaiming: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
        data object ResetToOnboarding : Event
    }
}