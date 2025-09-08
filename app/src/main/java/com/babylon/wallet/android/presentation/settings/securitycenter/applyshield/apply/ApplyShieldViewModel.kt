package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.usecases.transaction.PrepareApplyShieldRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val prepareApplyShieldRequestUseCase: PrepareApplyShieldRequestUseCase
) : StateViewModel<ApplyShieldViewModel.State>(),
    OneOffEventHandler<ApplyShieldViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onApplyClick(
        securityStructureId: SecurityStructureId,
        entityAddress: AddressOfAccountOrPersona
    ) = viewModelScope.launch {
        _state.update { state -> state.copy(isLoading = true) }

        prepareApplyShieldRequestUseCase(
            securityStructureId = securityStructureId,
            entityAddress = entityAddress
        ).onFailure { error ->
            _state.update { state ->
                state.copy(
                    isLoading = false,
                    message = UiMessage.ErrorMessage(error)
                )
            }
        }.onSuccess { request ->
            _state.update { state -> state.copy(isLoading = false) }
            sendEvent(Event.ShieldApplied)

            incomingRequestRepository.add(request)
        }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val isLoading: Boolean = false,
        val message: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object ShieldApplied : Event
    }
}
