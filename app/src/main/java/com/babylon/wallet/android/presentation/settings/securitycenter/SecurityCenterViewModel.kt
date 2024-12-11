package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.usecases.securityproblems.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    getSecurityProblemsUseCase: GetSecurityProblemsUseCase,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : StateViewModel<SecurityCenterViewModel.SecurityCenterUiState>(),
    OneOffEventHandler<SecurityCenterViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): SecurityCenterUiState {
        return SecurityCenterUiState.Loading
    }

    init {
        getSecurityProblemsUseCase()
            .onEach { securityProblems ->
                _state.emit(
                    SecurityCenterUiState.Data(
                        securityProblems = securityProblems
                    )
                )
            }
            .flowOn(dispatcher)
            .launchIn(viewModelScope)
    }

    fun onSecurityShieldsClick() {
        viewModelScope.launch {
            // TODO perform the actual check
            val hasSecurityShields = false
            sendEvent(
                if (hasSecurityShields) {
                    Event.ToSecurityShields
                } else {
                    Event.ToSecurityShieldsOnboarding
                }
            )
        }
    }

    sealed interface SecurityCenterUiState : UiState {

        data object Loading : SecurityCenterUiState

        data class Data(
            val securityProblems: Set<SecurityProblem>
        ) : SecurityCenterUiState {

            val hasSecurityProblems = securityProblems.isNotEmpty()

            val hasSecurityShieldsProblems = false

            val hasSecurityRelatedProblems = securityProblems.any { it.isSecurityFactorRelated }

            val hasCloudBackupProblems = securityProblems.any { it.hasCloudBackupProblems }
        }
    }

    sealed interface Event : OneOffEvent {

        data object ToSecurityShieldsOnboarding : Event

        data object ToSecurityShields : Event
    }
}
