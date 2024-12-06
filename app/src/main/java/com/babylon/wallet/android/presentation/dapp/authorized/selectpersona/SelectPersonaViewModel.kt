package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.LAST_USED_DATE_FORMAT_SHORT_MONTH
import com.babylon.wallet.android.utils.toEpochMillis
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.hex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SelectPersonaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) : StateViewModel<SelectPersonaViewModel.State>(), OneOffEventHandler<SelectPersonaViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SelectPersonaArgs(savedStateHandle)

    override fun initialState(): State = State()

    init {
        observeActivePersonasOnCurrentNetwork()
        getAuthorizedRequest()
    }

    private fun observeActivePersonasOnCurrentNetwork() {
        viewModelScope.launch {
            getProfileUseCase.flow
                .map { it.activePersonasOnCurrentNetwork }
                .collect { personas ->
                    val authorizedDApp = dAppConnectionRepository.getAuthorizedDApp(args.dappDefinitionAddress)
                    _state.update {
                        it.onProfileUpdated(
                            authorizedDApp = authorizedDApp,
                            profilePersonas = personas
                        )
                    }
                }
        }
    }

    private fun getAuthorizedRequest() {
        viewModelScope.launch {
            val interactionId = args.authorizedRequestInteractionId
            interactionId.let {
                val requestToHandle = incomingRequestRepository.getRequest(interactionId) as? WalletAuthorizedRequest
                if (requestToHandle == null) { // should never happen because
                    // the validation first occurs in the initialization of the DAppAuthorizedLoginViewModel
                    sendEvent(Event.TerminateFlow)
                    return@let
                } else {
                    _state.update { state ->
                        state.copy(walletAuthorizedRequest = requestToHandle)
                    }
                }
            }
        }
    }

    fun onPersonaSelected(personaAddress: IdentityAddress) {
        _state.update { it.onPersonaSelected(personaAddress) }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(Event.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    fun onContinueClick() {
        state.value.walletAuthorizedRequest?.let { request ->
            setSigningInProgress(true)

            viewModelScope.launch {
                val selectedPersonaEntity = state.value.selectedPersona?.asProfileEntity() ?: return@launch

                // check if signature is required
                val loginWithChallenge = request.loginWithChallenge
                if (loginWithChallenge != null) {
                    collectSignatures(
                        challenge = loginWithChallenge,
                        selectedPersonaEntity = selectedPersonaEntity,
                        metadata = request.metadata
                    )
                } else { // otherwise return the collected accounts without signatures
                    sendEvent(
                        Event.PersonaAuthorized(
                            persona = selectedPersonaEntity,
                            signature = null
                        )
                    )
                    setSigningInProgress(false)
                }
            }
        }
    }

    private suspend fun collectSignatures(
        challenge: Exactly32Bytes,
        selectedPersonaEntity: ProfileEntity.PersonaEntity,
        metadata: DappToWalletInteraction.RequestMetadata
    ) {
        val signRequest = SignRequest.RolaSignRequest(
            challengeHex = challenge.hex,
            origin = metadata.origin,
            dAppDefinitionAddress = metadata.dAppDefinitionAddress
        )

        accessFactorSourcesProxy.getSignatures(
            accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                signPurpose = SignPurpose.SignAuth,
                signRequest = signRequest,
                signers = listOf(selectedPersonaEntity.address)
            )
        ).onSuccess { result ->
            sendEvent(
                Event.PersonaAuthorized(
                    persona = selectedPersonaEntity,
                    signature = result.signersWithSignatures[selectedPersonaEntity]
                )
            )
            setSigningInProgress(false)
        }.onFailure {
            sendEvent(
                Event.AuthorizationFailed(throwable = RadixWalletException.DappRequestException.FailedToSignAuthChallenge(it))
            )
            setSigningInProgress(false)
        }
    }

    private fun setSigningInProgress(isEnabled: Boolean) = _state.update { it.copy(isSigningInProgress = isEnabled) }

    sealed interface Event : OneOffEvent {

        data object TerminateFlow : Event

        data class CreatePersona(val firstPersonaCreated: Boolean) : Event

        data class PersonaAuthorized(
            val persona: ProfileEntity.PersonaEntity,
            val signature: SignatureWithPublicKey? = null
        ) : Event

        data class AuthorizationFailed(val throwable: RadixWalletException) : Event
    }

    data class State(
        val isLoading: Boolean = true,
        val walletAuthorizedRequest: WalletAuthorizedRequest? = null,
        val personas: ImmutableList<PersonaUiModel> = persistentListOf(),
        val isSigningInProgress: Boolean = false,
        private val authorizedDApp: AuthorizedDapp? = null,
    ) : UiState {

        val selectedPersona: Persona? = personas.find { it.selected }?.persona

        val isFirstTimeLogin: Boolean
            get() = authorizedDApp == null

        val isContinueButtonEnabled: Boolean
            get() = selectedPersona != null

        fun onPersonaSelected(identityAddress: IdentityAddress): State = copy(
            personas = personas.map {
                it.copy(selected = identityAddress == it.persona.address)
            }.toImmutableList()
        )

        fun onProfileUpdated(
            authorizedDApp: AuthorizedDapp?,
            profilePersonas: List<Persona>,
        ): State {
            val authorizedPersonas = authorizedDApp?.referencesToAuthorizedPersonas

            val models = profilePersonas.map { persona ->
                val authorized = authorizedPersonas?.find { it.identityAddress == persona.address }

                persona.toUiModel().let { model ->
                    if (authorized != null) {
                        val localDateTime = authorized.lastLogin.toLocalDateTime()
                        model.copy(
                            lastUsedOn = localDateTime
                                ?.format(DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT_SHORT_MONTH)),
                            lastUsedOnTimestamp = localDateTime?.toEpochMillis() ?: 0
                        )
                    } else {
                        model
                    }
                }
            }.sortedByDescending {
                it.lastUsedOnTimestamp
            }

            val modelsWithSelectedInfo = if (selectedPersona == null) {
                // When the view model is first created, or no personas exists, no prior selection exists
                // so we decide to pre-select the first one
                models.mapIndexed { index, personaUiModel ->
                    personaUiModel.copy(selected = index == 0)
                }
            } else {
                val previousPersonaAddresses = personas.map { it.persona.address }.toSet()
                val currentPersonaAddresses = profilePersonas.map { it.address }.toSet()

                val personaToSelect = currentPersonaAddresses.minus(previousPersonaAddresses).firstOrNull()

                // When a newly created persona is detected, we preselect that one
                if (personaToSelect != null) {
                    models.map {
                        it.copy(selected = it.persona.address == personaToSelect)
                    }
                } else {
                    models
                }
            }

            return copy(
                authorizedDApp = authorizedDApp,
                personas = modelsWithSelectedInfo.toImmutableList(),
                isLoading = false
            )
        }
    }
}
