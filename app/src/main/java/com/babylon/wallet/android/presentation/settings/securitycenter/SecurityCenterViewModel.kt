package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.usecases.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val getSecurityProblemsUseCase: GetSecurityProblemsUseCase
) : StateViewModel<SecurityCenterViewModel.SecurityCenterUiState>() {

    override fun initialState(): SecurityCenterUiState {
        return SecurityCenterUiState()
    }

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

    data class SecurityCenterUiState(
        val securityProblems: Set<SecurityProblem>? = null
    ) : UiState {
        val hasSecurityProblems: Boolean
            get() = !securityProblems.isNullOrEmpty()
    }
}
