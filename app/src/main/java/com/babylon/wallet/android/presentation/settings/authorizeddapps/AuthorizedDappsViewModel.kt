package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class AuthorizedDappsViewModel @Inject constructor(
    private val getDAppsUseCase: GetDAppsUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository
) : StateViewModel<AuthorizedDappsUiState>() {

    override fun initialState(): AuthorizedDappsUiState = AuthorizedDappsUiState()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            dAppConnectionRepository.getAuthorizedDapps().collect { authorisedDApps ->
                val addresses = authorisedDApps.map { it.dAppDefinitionAddress }.toSet()
                getDAppsUseCase(
                    definitionAddresses = addresses,
                    needMostRecentData = false
                ).onSuccess { dApps ->
                    val result = authorisedDApps.mapNotNull { authorisedDApp ->
                        dApps.find { it.dAppAddress == authorisedDApp.dAppDefinitionAddress }
                    }

                    _state.update { it.copy(dApps = result.toImmutableList(), isLoading = false) }
                }.onFailure { error ->
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error), isLoading = false) }
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }
}

data class AuthorizedDappsUiState(
    val dApps: ImmutableList<DApp> = persistentListOf(),
    val isLoading: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState
