package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppWithAssociatedResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.usecases.GetDAppWithAssociatedResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.dappdetail.DappDetailEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle,
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithAssociatedResourcesUseCase
) : StateViewModel<PersonaDetailUiState>(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)

    private val authorizedDApps = dAppConnectionRepository.getAuthorizedDappsByPersona(args.personaAddress)

    override fun initialState(): PersonaDetailUiState = PersonaDetailUiState()

    init {
        viewModelScope.launch {
            authorizedDApps.collect { authorizedDApps ->
                val metadataResults = authorizedDApps.map { authorizedDApp ->
                    dAppWithAssociatedResourcesUseCase.invoke(
                        definitionAddress = authorizedDApp.dAppDefinitionAddress,
                        needMostRecentData = false
                    ).value()
                }
                val dApps = metadataResults.mapNotNull { dAppWithAssociatedResources ->
                    dAppWithAssociatedResources
                }

                _state.update { state ->
                    state.copy(
                        persona = getProfileUseCase.personaOnCurrentNetwork(args.personaAddress),
                        loading = false,
                        authorizedDapps = dApps.toImmutableList()
                    )
                }
            }
        }
    }

    fun onDAppClick(dApp: DAppWithAssociatedResources) {
        _state.update { state ->
            state.copy(
                selectedDAppWithMetadata = dApp.dAppWithMetadata,
                selectedDAppAssociatedFungibleTokens = dApp.fungibleResources.toPersistentList(),
                selectedDAppAssociatedNonFungibleTokens = dApp.nonFungibleResources.toPersistentList()
            )
        }
    }
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<DAppWithAssociatedResources> = persistentListOf(),
    val persona: Network.Persona? = null,
    val selectedDAppWithMetadata: DAppWithMetadata? = null,
    val selectedDAppAssociatedFungibleTokens: ImmutableList<Resource.FungibleResource> = persistentListOf(),
    val selectedDAppAssociatedNonFungibleTokens: ImmutableList<Resource.NonFungibleResource> = persistentListOf(),
) : UiState
