package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccessFactorSourcesViewModel @Inject constructor() : StateViewModel<AccessFactorSourcesViewModel.AccessFactorSourceUiState>() {

    override fun initialState(): AccessFactorSourceUiState = AccessFactorSourceUiState()

    @Suppress("UnusedParameter") // will be used later
    fun biometricAuthenticationCompleted(isAuthenticated: Boolean) {
        // todo ...
    }

    @Suppress("UnusedPrivateMember") // will be used later
    private fun biometricAuthenticationDismissed() {
        // todo ...
    }

    data class AccessFactorSourceUiState(
        val loading: Boolean = false
    ) : UiState
}
