package com.babylon.wallet.android.presentation.claimed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.CloudBackupErrorStream
import javax.inject.Inject

@HiltViewModel
class ClaimedByAnotherDeviceViewModel @Inject constructor(
    private val cloudBackupErrorStream: CloudBackupErrorStream
) : ViewModel(), OneOffEventHandler<ClaimedByAnotherDeviceViewModel.Event> by OneOffEventHandlerImpl() {

    fun onModalAcknowledged() = viewModelScope.launch {
        cloudBackupErrorStream.resetErrors()
        sendEvent(Event.ResetToOnboarding)
    }

    sealed interface Event : OneOffEvent {
        data object ResetToOnboarding : Event
    }
}
