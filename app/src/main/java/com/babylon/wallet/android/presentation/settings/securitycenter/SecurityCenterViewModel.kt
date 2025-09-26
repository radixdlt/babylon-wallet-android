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
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    getSecurityProblemsUseCase: GetSecurityProblemsUseCase,
    getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SecurityCenterViewModel.State>(),
    OneOffEventHandler<SecurityCenterViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(isLoading = true)

    init {
        getSecurityProblemsUseCase()
            .onEach { securityProblems ->
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        securityProblems = securityProblems
                    )
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)

        getProfileUseCase.flow.map { it.currentNetwork?.id == NetworkId.STOKENET }
            .onEach { isOnStokenet ->
                _state.update { state ->
                    state.copy(
                        mfaEnabled = isOnStokenet
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onSecurityShieldsClick() {
        viewModelScope.launch {
            val hasSecurityShields = sargonOsManager.callSafely(defaultDispatcher) {
                getShieldsForDisplay().isNotEmpty()
            }.getOrElse { false }
            sendEvent(
                if (hasSecurityShields) {
                    Event.ToSecurityShields
                } else {
                    Event.ToCreateSecurityShield
                }
            )
        }
    }

    data class State(
        val isLoading: Boolean,
        val securityProblems: Set<SecurityProblem> = emptySet(),
        val mfaEnabled: Boolean = false
    ) : UiState {

        val hasSecurityProblems = securityProblems.isNotEmpty()

        val hasSecurityRelatedProblems = securityProblems.any { it.isSecurityFactorRelated }

        val hasCloudBackupProblems = securityProblems.any { it.hasCloudBackupProblems }
    }

    sealed interface Event : OneOffEvent {

        data object ToCreateSecurityShield : Event

        data object ToSecurityShields : Event
    }
}
