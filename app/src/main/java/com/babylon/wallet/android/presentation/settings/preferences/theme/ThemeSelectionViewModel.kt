package com.babylon.wallet.android.presentation.settings.preferences.theme

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.ThemeSelection
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class ThemeSelectionViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : StateViewModel<ThemeSelectionViewModel.State>(),
    OneOffEventHandler<ThemeSelectionViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(
        selection = ThemeSelection.DEFAULT
    )

    init {
        viewModelScope.launch {
            preferencesManager.themeSelection
                .distinctUntilChanged()
                .collect { selection ->
                    _state.update { it.copy(selection = selection) }
                }
        }
    }

    fun onThemeSelected(selection: ThemeSelection) {
        viewModelScope.launch {
            preferencesManager.setThemeSelection(selection)
            sendEvent(Event.Dismiss)
        }
    }

    data class State(
        val selection: ThemeSelection
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
}
