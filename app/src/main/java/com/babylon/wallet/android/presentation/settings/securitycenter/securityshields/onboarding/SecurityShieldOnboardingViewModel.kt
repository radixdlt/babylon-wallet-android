package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityShieldOnboardingViewModel @Inject constructor() :
    StateViewModel<SecurityShieldOnboardingViewModel.State>(),
    OneOffEventHandler<SecurityShieldOnboardingViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onButtonClick() {
        if (state.value.isLastPage) {
            viewModelScope.launch {
                // TODO perform the actual check
                val hasEnoughFactors = false

                sendEvent(
                    if (hasEnoughFactors) {
                        Event.SelectFactors
                    } else {
                        Event.SetupFactors
                    }
                )
            }
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

    data class State(
        val pages: List<Page> = Page.entries.toList(),
        val currentPagePosition: Int = 0,
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
