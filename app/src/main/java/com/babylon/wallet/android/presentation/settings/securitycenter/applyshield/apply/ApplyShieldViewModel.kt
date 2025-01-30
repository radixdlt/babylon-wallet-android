package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<ApplyShieldViewModel.State>(),
    OneOffEventHandler<ApplyShieldViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onApplyClick(
        securityStructureId: SecurityStructureId,
        entityAddresses: List<AddressOfAccountOrPersona>
    ) = viewModelScope.launch {
        _state.update { state -> state.copy(isLoading = true) }

        sargonOsManager.callSafely(dispatcher) { applySecurityShieldWithIdToEntities(securityStructureId, entityAddresses) }
            .onSuccess {
                _state.update { state -> state.copy(isLoading = false) }
                sendEvent(Event.ShieldApplied)
            }
            .onFailure { error ->
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        message = UiMessage.ErrorMessage(error)
                    )
                }
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
