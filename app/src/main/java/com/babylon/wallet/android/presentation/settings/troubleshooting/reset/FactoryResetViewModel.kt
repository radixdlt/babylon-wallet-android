package com.babylon.wallet.android.presentation.settings.troubleshooting.reset

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.usecases.DeleteWalletUseCase
import com.babylon.wallet.android.domain.usecases.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FactoryResetViewModel @Inject constructor(
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val getSecurityProblemsUseCase: GetSecurityProblemsUseCase
) : StateViewModel<FactoryResetViewModel.State>(), OneOffEventHandler<FactoryResetViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            getSecurityProblemsUseCase().collect { problems ->
                _state.update { state ->
                    state.copy(
                        securityProblems = problems
                    )
                }
            }
        }
    }

    fun onDeleteWalletClick() {
        _state.update { it.copy(deleteWalletDialogVisible = true) }
    }

    fun onDeleteWalletConfirm() {
        _state.update { it.copy(deleteWalletDialogVisible = false) }

        viewModelScope.launch {
            deleteWalletUseCase()
            sendEvent(Event.ProfileDeleted)
        }
    }

    fun onDeleteWalletDeny() {
        _state.update { it.copy(deleteWalletDialogVisible = false) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val securityProblems: Set<SecurityProblem>? = null,
        val deleteWalletDialogVisible: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object ProfileDeleted : Event
    }
}
