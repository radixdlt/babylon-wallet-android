package com.babylon.wallet.android.presentation.settings.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.getStringInstructions
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.utils.hasAuthSigning
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import rdx.works.profile.domain.personaOnCurrentNetworkFlow
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    getProfileUseCase: GetProfileUseCase,
    private val appEventBus: AppEventBus,
    private val addAuthSigningFactorInstanceUseCase: AddAuthSigningFactorInstanceUseCase,
    private val rolaClient: ROLAClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    savedStateHandle: SavedStateHandle
) : StateViewModel<PersonaDetailUiState>() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)
    private var authSigningFactorInstance: FactorInstance? = null
    private lateinit var uploadAuthKeyRequestId: String

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.personaOnCurrentNetworkFlow(args.personaAddress),
                dAppConnectionRepository.getAuthorizedDappsByPersona(args.personaAddress)
            ) { persona, dapps ->
                persona to dapps
            }.collect { personaToDapps ->
                _state.update { state ->
                    state.copy(
                        persona = personaToDapps.first,
                        authorizedDapps = personaToDapps.second.toPersistentList(),
                        loading = false,
                        hasAuthKey = personaToDapps.first.hasAuthSigning()
                    )
                }
            }
        }
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.TransactionEvent>().filter { it.requestId == uploadAuthKeyRequestId }
                .collect { event ->
                    when (event) {
                        is AppEvent.TransactionEvent.Failed -> {
                            _state.update { it.copy(loading = false) }
                        }
                        is AppEvent.TransactionEvent.Successful -> {
                            val persona = requireNotNull(state.value.persona)
                            val authSigningFactorInstance = requireNotNull(authSigningFactorInstance)
                            addAuthSigningFactorInstanceUseCase(persona, authSigningFactorInstance)
                            _state.update { it.copy(loading = false) }
                        }
                        else -> {}
                    }
                }
        }
    }

    override fun initialState(): PersonaDetailUiState {
        return PersonaDetailUiState()
    }

    fun onCreateAndUploadAuthKey() {
        viewModelScope.launch {
            state.value.persona?.let { persona ->
                _state.update { it.copy(loading = true) }
                val authSigningFactorInstance = rolaClient.generateAuthSigningFactorInstance(persona)
                this@PersonaDetailViewModel.authSigningFactorInstance = authSigningFactorInstance
                rolaClient.createAuthKeyManifestWithStringInstructions(persona, authSigningFactorInstance)?.let { manifest ->
                    uploadAuthKeyRequestId = UUIDGenerator.uuid().toString()
                    val internalMessage = MessageFromDataChannel.IncomingRequest.TransactionRequest(
                        dappId = "",
                        requestId = uploadAuthKeyRequestId,
                        transactionManifestData = TransactionManifestData(
                            instructions = requireNotNull(manifest.getStringInstructions()),
                            version = TransactionVersion.Default.value,
                            networkId = persona.networkID,
                            blobs = manifest.blobs?.toList().orEmpty()
                        ),
                        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(persona.networkID)
                    )
                    incomingRequestRepository.add(internalMessage)
                }
            }
        }
    }
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<Network.AuthorizedDapp> = persistentListOf(),
    val persona: Network.Persona? = null,
    val hasAuthKey: Boolean = false
) : UiState
