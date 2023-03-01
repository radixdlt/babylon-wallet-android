package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.settings.dappdetail.DappDetailEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import javax.inject.Inject

@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    personaRepository: PersonaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)

    val state = combine(
        personaRepository.getPersonaByAddressFlow(args.personaAddress),
        dAppConnectionRepository.getAuthorizedDappsByPersona(args.personaAddress)
    ) { persona, dapps ->
        PersonaDetailUiState(
            persona = persona,
            authorizedDapps = dapps.toPersistentList(),
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS), PersonaDetailUiState())
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<OnNetwork.AuthorizedDapp> = persistentListOf(),
    val persona: OnNetwork.Persona? = null
)
