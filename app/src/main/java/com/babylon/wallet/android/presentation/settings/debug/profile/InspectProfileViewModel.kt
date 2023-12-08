package com.babylon.wallet.android.presentation.settings.debug.profile

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.debug.profile.InspectProfileViewModel.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.annotations.DebugOnly
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.prettyPrinted
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class InspectProfileViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
) : StateViewModel<State>() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            getProfileUseCase().collect { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
    }

    fun toggleRawProfileVisible(isRawProfileVisible: Boolean) {
        _state.update { it.copy(isRawProfileVisible = isRawProfileVisible) }
    }

    data class State(
        val profile: Profile? = null,
        val isRawProfileVisible: Boolean = true // Currently default viewer is raw json
    ) : UiState {
        @OptIn(DebugOnly::class)
        val rawSnapshot: String? by lazy {
            profile?.prettyPrinted()
        }
    }
}
