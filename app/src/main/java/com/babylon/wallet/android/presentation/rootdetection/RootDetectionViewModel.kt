package com.babylon.wallet.android.presentation.rootdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class RootDetectionViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel(), OneOffEventHandler<RootDetectionEvent> by OneOffEventHandlerImpl() {

    fun onAcknowledgeClick() {
        viewModelScope.launch {
            // Switch val in datastore
            preferencesManager.markDeviceRootedDialogShown()
            sendEvent(RootDetectionEvent.RootAcknowledged)
        }
    }
}

internal sealed interface RootDetectionEvent : OneOffEvent {
    data object RootAcknowledged : RootDetectionEvent
}
