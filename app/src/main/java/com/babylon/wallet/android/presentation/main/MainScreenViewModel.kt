package com.babylon.wallet.android.presentation.main

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileStateUseCase
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    getProfileStateUseCase: GetProfileStateUseCase,
) : StateViewModel<MainScreenState>() {

    init {
        viewModelScope.launch {
            getProfileStateUseCase().collect { profileState ->
                _state.update {
                    MainScreenState(
                        initialAppState = AppState.from(
                            profileState = profileState
                        )
                    )
                }
            }
        }
    }

    override fun initialState(): MainScreenState {
        return MainScreenState()
    }
}

data class MainScreenState(
    val initialAppState: AppState = AppState.Loading
) : UiState
