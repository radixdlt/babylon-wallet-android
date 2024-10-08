package com.babylon.wallet.android.presentation.incompatibleprofile

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.DeleteWalletUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.errorCodeFromError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class IncompatibleProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val deviceCapabilityHelper: DeviceCapabilityHelper
) : StateViewModel<IncompatibleProfileViewModel.State>(),
    OneOffEventHandler<IncompatibleProfileViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val incompatibleProfile = profileRepository.profileState.filterIsInstance<ProfileState.Incompatible>().firstOrNull()
            _state.update { it.copy(incompatibleCause = incompatibleProfile?.v1) }
        }
    }

    fun sendLogsToSupportClick() {
        viewModelScope.launch {
            val body = StringBuilder()
            body.append(deviceCapabilityHelper.supportEmailTemplate)
            body.appendLine("=========================")
            body.appendLine("Incompatible Wallet Profile")

            val cause = _state.value.incompatibleCause
            if (cause != null) {
                body.appendLine("Error Code: ${errorCodeFromError(cause)}")

                if (cause is CommonException.FailedToDeserializeJsonToValue) {
                    body.appendLine(_state.value.incompatibleCause?.message)
                    body.appendLine()
                    body.appendLine(_state.value.incompatibleCause?.stackTraceToString())
                }
            }
            sendEvent(Event.OnSendLogsToSupport(body = body.toString()))
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            deleteWalletUseCase()
            sendEvent(Event.ProfileDeleted)
        }
    }

    data class State(
        val incompatibleCause: CommonException? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object ProfileDeleted : Event
        data class OnSendLogsToSupport(
            val body: String
        ) : Event
    }
}
