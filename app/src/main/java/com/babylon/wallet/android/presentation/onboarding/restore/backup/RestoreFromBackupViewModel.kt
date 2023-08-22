package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class RestoreFromBackupViewModel @Inject constructor(
    profileRepository: ProfileRepository
) : StateViewModel<RestoreFromBackupViewModel.State>(), OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToRestore = profileRepository.getRestoringProfileFromBackup()
            _state.update { it.copy(restoringProfile = profileToRestore) }
        }
    }

    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfile?.header?.isCompatible == true) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        sendEvent(Event.OnDismiss)
    }

    fun onSubmit() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            sendEvent(Event.OnRestoreConfirm)
        }
    }

    data class State(
        val restoringProfile: Profile? = null,
        val isRestoringProfileChecked: Boolean = false
    ) : UiState {

        val isContinueEnabled: Boolean
            get() = isRestoringProfileChecked
    }

    sealed interface Event : OneOffEvent {
        object OnDismiss : Event
        object OnRestoreConfirm : Event
    }
}
