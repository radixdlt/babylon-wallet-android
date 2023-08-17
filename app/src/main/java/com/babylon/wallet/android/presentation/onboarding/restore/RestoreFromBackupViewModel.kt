package com.babylon.wallet.android.presentation.onboarding.restore

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@HiltViewModel
class RestoreFromBackupViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase
) : StateViewModel<RestoreFromBackupViewModel.State>(), OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToRestore = profileRepository.getRestoredProfileFromBackup()
            _state.update { it.copy(restoringProfileHeader = profileToRestore?.header) }
        }
    }

    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfileHeader?.isCompatible == true) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        sendEvent(Event.OnDismiss)
    }

    fun onSubmit() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            restoreProfileFromBackupUseCase()
            sendEvent(Event.OnRestored)
        }
    }

    data class State(
        val restoringProfileHeader: Header? = null,
        val isRestoringProfileChecked: Boolean = false
    ) : UiState {

        val isContinueEnabled: Boolean
            get() = isRestoringProfileChecked

    }

    sealed interface Event : OneOffEvent {
        object OnDismiss : Event
        object OnRestored : Event
    }
}
