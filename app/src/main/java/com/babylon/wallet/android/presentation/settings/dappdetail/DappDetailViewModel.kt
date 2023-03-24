package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import javax.inject.Inject

@HiltViewModel
class DappDetailViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dappMetadataRepository: DappMetadataRepository,
    private val personaRepository: PersonaRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private lateinit var authorizedDapp: Network.AuthorizedDapp
    private val args = DappDetailScreenArgs(savedStateHandle)

    private val _state: MutableStateFlow<DappDetailUiState> =
        MutableStateFlow(DappDetailUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val metadataResult = dappMetadataRepository.getDappMetadata(
                defitnionAddress = args.dappDefinitionAddress,
                needMostRecentData = false
            )
            metadataResult.onValue { metadata ->
                _state.update { state ->
                    state.copy(dappMetadata = metadata, loading = false)
                }
            }
            metadataResult.onError {
                _state.update { state ->
                    state.copy(loading = false)
                }
            }
        }
        observeDapp()
    }

    private fun observeDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.getAuthorizedDappFlow(args.dappDefinitionAddress).collect {
                if (it == null) {
                    sendEvent(DappDetailEvent.LastPersonaDeleted)
                    return@collect
                } else {
                    authorizedDapp = checkNotNull(dAppConnectionRepository.getAuthorizedDapp(args.dappDefinitionAddress))
                }
                val personas = authorizedDapp.referencesToAuthorizedPersonas.mapNotNull { personaSimple ->
                    personaRepository.getPersonaByAddress(personaSimple.identityAddress)
                }
                _state.update { state ->
                    val selectedPersona = personas.firstOrNull {
                        it.address == state.selectedPersona?.address
                    } ?: state.selectedPersona
                    state.copy(
                        dapp = authorizedDapp,
                        personas = personas.toPersistentList(),
                        selectedPersona = selectedPersona
                    )
                }
            }
        }
    }

    fun onPersonaClick(persona: Network.Persona) {
        viewModelScope.launch {
            val personaSimple =
                authorizedDapp.referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == persona.address }
            val sharedAccounts = personaSimple?.sharedAccounts?.accountsReferencedByAddress?.mapNotNull {
                accountRepository.getAccountByAddress(it)?.toUiModel()
            }.orEmpty()
            _state.update {
                it.copy(selectedPersona = persona, sharedPersonaAccounts = sharedAccounts.toPersistentList())
            }
        }
    }

    fun onPersonaDetailsClosed() {
        _state.update {
            it.copy(selectedPersona = null, sharedPersonaAccounts = persistentListOf())
        }
    }

    fun onDisconnectPersona(persona: Network.Persona) {
        viewModelScope.launch {
            dAppConnectionRepository.deletePersonaForDapp(args.dappDefinitionAddress, persona.address)
        }
    }

    fun onDeleteDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.deleteAuthorizedDapp(args.dappDefinitionAddress)
            sendEvent(DappDetailEvent.DappDeleted)
        }
    }
}

sealed interface DappDetailEvent : OneOffEvent {
    object LastPersonaDeleted : DappDetailEvent
    object DappDeleted : DappDetailEvent
}

data class DappDetailUiState(
    val loading: Boolean = true,
    val dapp: Network.AuthorizedDapp? = null,
    val dappMetadata: DappMetadata? = null,
    val personas: ImmutableList<Network.Persona> = persistentListOf(),
    val selectedPersona: Network.Persona? = null,
    val sharedPersonaAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
)
