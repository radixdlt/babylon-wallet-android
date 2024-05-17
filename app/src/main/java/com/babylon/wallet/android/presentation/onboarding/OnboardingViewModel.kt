package com.babylon.wallet.android.presentation.onboarding

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import rdx.works.profile.cloudbackup.BackupServiceException
import rdx.works.profile.cloudbackup.DriveClient
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val driveClient: DriveClient
) : StateViewModel<OnboardingViewModel.State>() {

    override fun initialState(): State = State(
        isProfileClaimedByAnotherDeviceWarningVisible = driveClient.backupErrors.value
        is BackupServiceException.ProfileClaimedByAnotherDeviceException
    )

    fun claimedByAnotherDeviceWarningAcknowledged() {
        _state.update { it.copy(isProfileClaimedByAnotherDeviceWarningVisible = false) }
    }

    data class State(
        val isProfileClaimedByAnotherDeviceWarningVisible: Boolean = false
    ) : UiState
}
