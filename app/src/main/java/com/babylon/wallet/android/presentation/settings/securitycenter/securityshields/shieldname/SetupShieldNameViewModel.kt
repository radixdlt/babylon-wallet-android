package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.extensions.SharedConstants
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupShieldNameViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SetupShieldNameViewModel.State>(), OneOffEventHandler<SetupShieldNameViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onNameChange(value: String) {
        _state.update { state ->
            state.copy(
                name = value
            )
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch(defaultDispatcher) {
            val securityStructureOfFactorSourceIDs = securityShieldBuilderClient.buildShield(state.value.name)

            sargonOsManager.callSafely(defaultDispatcher) {
                addSecurityStructureOfFactorSourceIds(securityStructureOfFactorSourceIDs)
            }.onSuccess {
                sendEvent(Event.ShieldCreated)
            }.onFailure {
                _state.update { state -> state.copy(message = UiMessage.ErrorMessage(it)) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val name: String = "",
        val message: UiMessage? = null
    ) : UiState {

        val isNameTooLong = name.trim().length > SharedConstants.displayNameMaxLength
        val isButtonEnabled = name.isNotBlank()
    }

    sealed interface Event : OneOffEvent {

        data object ShieldCreated : Event
    }
}
