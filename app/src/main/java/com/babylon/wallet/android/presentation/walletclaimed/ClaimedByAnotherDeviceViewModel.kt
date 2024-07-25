package com.babylon.wallet.android.presentation.walletclaimed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.extensions.from
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.claim
import rdx.works.profile.cloudbackup.data.DriveClient
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

@HiltViewModel
class ClaimedByAnotherDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cloudBackupErrorStream: CloudBackupErrorStream,
    private val profileRepository: ProfileRepository,
    private val driveClient: DriveClient,
    private val hostInfoRepository: HostInfoRepository
) : StateViewModel<ClaimedByAnotherDeviceViewModel.State>(),
    OneOffEventHandler<ClaimedByAnotherDeviceViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ClaimedByAnotherDeviceArgs(savedStateHandle)

    override fun initialState(): State = State()

    fun onClearWalletClick() = viewModelScope.launch {
        profileRepository.clearAllWalletData()
        cloudBackupErrorStream.resetErrors()
        sendEvent(Event.ResetToOnboarding)
    }

    fun onTransferWalletBackClick() = viewModelScope.launch {
        val currentProfile = profileRepository.profile.firstOrNull() ?: return@launch

        _state.update { it.copy(isReclaiming = true) }
        val hostId = hostInfoRepository.getHostId()
        val hostInfo = hostInfoRepository.getHostInfo()
        val claimingProfile = currentProfile.claim(deviceInfo = DeviceInfo.from(hostId, hostInfo))

        driveClient.claimCloudBackup(
            file = args.claimedEntity,
            claimingProfile = claimingProfile
        ).onSuccess {
            cloudBackupErrorStream.resetErrors()
            sendEvent(Event.Reclaimed)
        }.onFailure {
            _state.update { state -> state.copy(isReclaiming = false) }
        }
    }

    data class State(
        val isReclaiming: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Reclaimed : Event
        data object ResetToOnboarding : Event
    }
}
