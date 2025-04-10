package com.babylon.wallet.android.presentation.dapp.authorized.verifyentities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class VerifyEntitiesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val signAuthUseCase: SignAuthUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<VerifyEntitiesViewModel.State>(),
    OneOffEventHandler<VerifyEntitiesViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    private val args = VerifyEntitiesArgs(savedStateHandle)

    init {
        viewModelScope.launch(defaultDispatcher) {
            getAuthorizedRequest()
            getRequestedEntitiesToVerify()
        }
    }

    private suspend fun getAuthorizedRequest() {
        val interactionId = args.authorizedRequestInteractionId
        interactionId.let {
            val requestToHandle = incomingRequestRepository.getRequest(interactionId) as? WalletAuthorizedRequest
            if (requestToHandle == null) { // should never happen because
                // the validation first occurs in the initialization of the DAppAuthorizedLoginViewModel
                sendEvent(Event.TerminateVerification)
                return@let
            } else {
                _state.update { state ->
                    state.copy(
                        walletAuthorizedRequest = requestToHandle,
                        canNavigateBack = args.canNavigateBack
                    )
                }
            }
        }
    }

    private suspend fun getRequestedEntitiesToVerify() {
        val profile = getProfileUseCase()
        val entitiesForProofWithSignatures = args.entitiesForProofWithSignatures
        val activeAccountsOnCurrentNetwork = profile.activeAccountsOnCurrentNetwork

        _state.update { state ->
            state.copy(
                requestedPersona = entitiesForProofWithSignatures.personaAddress?.let { identityAddress ->
                    profile.activePersonaOnCurrentNetwork(identityAddress)?.asProfileEntity()
                },
                requestedAccounts = activeAccountsOnCurrentNetwork.filter { activeAccount ->
                    activeAccount.address in entitiesForProofWithSignatures.accountAddresses
                }.map { it.asProfileEntity() },
                collectedSignatures = entitiesForProofWithSignatures.signatures
            )
        }
    }

    fun onContinueClick() {
        state.value.walletAuthorizedRequest?.let { request ->
            viewModelScope.launch {
                val item = request.proofOfOwnershipRequestItem ?: return@launch

                val requestedPersona = state.value.requestedPersona
                if (requestedPersona != null) {
                    signPersona(
                        interactionId = request.interactionId,
                        challenge = item.challenge,
                        metadata = request.requestMetadata,
                        persona = requestedPersona
                    )
                } else {
                    signAccounts(
                        challenge = item.challenge,
                        metadata = request.requestMetadata,
                        accounts = state.value.requestedAccounts
                    )
                }
            }
        }
    }

    private suspend fun signPersona(
        interactionId: WalletInteractionId,
        challenge: Exactly32Bytes,
        metadata: DappToWalletInteraction.RequestMetadata,
        persona: ProfileEntity.PersonaEntity
    ) {
        setSigningInProgress(true)
        signAuthUseCase.persona(
            challenge = challenge,
            persona = persona.persona,
            metadata = metadata
        ).onSuccess { signature ->
            _state.update { state ->
                state.copy(collectedSignatures = mapOf(persona.address to signature))
            }

            if (state.value.requestedAccounts.isNotEmpty()) {
                sendEvent(
                    Event.NavigateToVerifyAccounts(
                        walletAuthorizedRequestInteractionId = interactionId,
                        entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                            accountAddresses = state.value.requestedAccounts.map { it.accountAddress },
                            signatures = mapOf(
                                AddressOfAccountOrPersona.Identity(persona.identityAddress) to signature
                            )
                        )
                    )
                )
            }
            setSigningInProgress(false)
        }.onFailure {
            sendEvent(
                Event.AuthorizationFailed(
                    throwable = RadixWalletException.DappRequestException.FailedToSignAuthChallenge
                )
            )
            setSigningInProgress(false)
        }
    }

    private suspend fun signAccounts(
        challenge: Exactly32Bytes,
        metadata: DappToWalletInteraction.RequestMetadata,
        accounts: List<ProfileEntity.AccountEntity>
    ) {
        if (accounts.isNotEmpty()) {
            setSigningInProgress(true)
            signAuthUseCase.accounts(
                challenge = challenge,
                accounts = accounts.map { it.account },
                metadata = metadata
            ).onSuccess { signatures ->
                _state.update { state ->
                    state.addSignatures(signatures.mapKeys { AddressOfAccountOrPersona.Account(it.key.address) })
                }

                sendEvent(state.value.entitiesVerifiedEvent())
                setSigningInProgress(false)
            }.onFailure {
                sendEvent(
                    Event.AuthorizationFailed(
                        throwable = RadixWalletException.DappRequestException.FailedToSignAuthChallenge
                    )
                )
                setSigningInProgress(false)
            }
        } else {
            sendEvent(state.value.entitiesVerifiedEvent())
        }
    }

    private fun setSigningInProgress(isEnabled: Boolean) = _state.update { it.copy(isSigningInProgress = isEnabled) }

    data class State(
        val walletAuthorizedRequest: WalletAuthorizedRequest? = null,
        val requestedPersona: ProfileEntity.PersonaEntity? = null,
        val requestedAccounts: List<ProfileEntity.AccountEntity> = emptyList(),
        private val collectedSignatures: Map<AddressOfAccountOrPersona, SignatureWithPublicKey> = emptyMap(),
        val canNavigateBack: Boolean = false,
        val isSigningInProgress: Boolean = false
    ) : UiState {

        enum class EntityType {
            Persona, Account
        }

        val entityType: EntityType
            get() = if (requestedPersona != null) {
                EntityType.Persona
            } else {
                EntityType.Account
            }

        val nextEntitiesForProof: List<ProfileEntity>
            get() = if (requestedPersona != null) {
                listOf(requestedPersona)
            } else if (requestedAccounts.isNotEmpty()) {
                requestedAccounts
            } else {
                emptyList()
            }

        fun addSignatures(signatures: Map<AddressOfAccountOrPersona, SignatureWithPublicKey>) = copy(
            collectedSignatures = this.collectedSignatures.toMutableMap().apply {
                putAll(signatures)
            }
        )

        fun entitiesVerifiedEvent() = Event.EntitiesVerified(collectedSignatures)
    }

    sealed interface Event : OneOffEvent {

        data object TerminateVerification : Event

        data class EntitiesVerified(val signatures: Map<AddressOfAccountOrPersona, SignatureWithPublicKey>) : Event

        data class NavigateToVerifyAccounts(
            val walletAuthorizedRequestInteractionId: String,
            val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
        ) : Event

        data class AuthorizationFailed(val throwable: RadixWalletException) : Event
    }
}
