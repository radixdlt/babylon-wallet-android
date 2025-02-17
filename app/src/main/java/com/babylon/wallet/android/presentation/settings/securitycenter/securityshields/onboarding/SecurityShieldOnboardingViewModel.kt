package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.SecurityShieldPrerequisitesStatus
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityShieldOnboardingViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<SecurityShieldOnboardingViewModel.State>(),
    OneOffEventHandler<SecurityShieldOnboardingViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onButtonClick() {
        if (state.value.isLastPage) {
            navigateNext()
        } else {
            _state.update { state ->
                state.copy(
                    currentPagePosition = state.currentPagePosition + 1
                )
            }
        }
    }

    fun onPageChange(page: Int) {
        _state.update { state ->
            state.copy(
                currentPagePosition = page
            )
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun navigateNext() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                val status = securityShieldPrerequisitesStatus()
                status == SecurityShieldPrerequisitesStatus.SUFFICIENT
            }.onSuccess { hasEnoughFactors ->
                sendEvent(
                    if (hasEnoughFactors) {
                        Event.SelectFactors
                    } else {
                        Event.SetupFactors
                    }
                )
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        message = UiMessage.ErrorMessage(it)
                    )
                }
            }
        }
    }

    data class State(
        val pages: List<Page> = Page.entries.toList(),
        val currentPagePosition: Int = 0,
        val message: UiMessage? = null
    ) : UiState {

        val pageCount: Int = pages.size

        val isLastPage = currentPagePosition == pageCount - 1

        enum class Page {

            Introduction,
            AddFactors,
            ApplyShield
        }
    }

    sealed interface Event : OneOffEvent {

        data object SelectFactors : Event

        data object SetupFactors : Event
    }
}
