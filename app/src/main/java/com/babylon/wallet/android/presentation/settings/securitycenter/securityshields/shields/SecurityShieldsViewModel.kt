package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shields

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.securityshields.GetSecurityShieldCardsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityShieldsViewModel @Inject constructor(
    private val getSecurityShieldCardsUseCase: GetSecurityShieldCardsUseCase,
    getProfileUseCase: GetProfileUseCase
) : StateViewModel<SecurityShieldsViewModel.State>() {

    override fun initialState(): State = State(isLoading = true)

    init {
        getProfileUseCase.flow
            .map { it.appPreferences.security.securityStructuresOfFactorSourceIds }
            .distinctUntilChanged()
            .onEach { initSecurityShields() }
            .launchIn(viewModelScope)
    }

    fun onDismissMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    private fun initSecurityShields() {
        viewModelScope.launch {
            getSecurityShieldCardsUseCase()
                .onFailure { error ->
                    Timber.e("Failed to get security shields for display: $error")

                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = UiMessage.ErrorMessage(error)
                        )
                    }
                }.onSuccess { securityShields ->
                    _state.update { state ->
                        state.copy(
                            shields = securityShields.toPersistentList(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    data class State(
        val isLoading: Boolean,
        val shields: List<SecurityShieldCard> = persistentListOf(),
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState
}
