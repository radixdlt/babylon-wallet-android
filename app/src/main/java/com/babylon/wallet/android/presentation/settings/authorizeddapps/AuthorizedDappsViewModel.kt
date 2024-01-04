package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
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
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithResourcesUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository
) : StateViewModel<AuthorizedDappsUiState>() {

    override fun initialState(): AuthorizedDappsUiState = AuthorizedDappsUiState()

    init {
        viewModelScope.launch {
            dAppConnectionRepository.getAuthorizedDapps().collect {
                val dApps = it.mapNotNull { dApp ->
                    val metadataResult = dAppWithAssociatedResourcesUseCase.invoke(
                        definitionAddress = dApp.dAppDefinitionAddress,
                        needMostRecentData = false
                    )
                    metadataResult.getOrNull()
                }
                _state.update { state ->
                    state.copy(
                        dApps = dApps.toImmutableList()
                    )
                }
            }
        }
    }
}

data class AuthorizedDappsUiState(
    val dApps: ImmutableList<DAppWithResources> = persistentListOf()
) : UiState
