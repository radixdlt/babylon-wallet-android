package com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.hex
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
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<VerifyEntitiesViewModel.State>(),
    OneOffEventHandler<VerifyEntitiesViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    private val args = VerifyEntitiesArgs(savedStateHandle)

    init {
        viewModelScope.launch(defaultDispatcher) {
            getUnauthorizedRequest()
            getRequestedEntitiesToVerify()
        }
    }

    private suspend fun getUnauthorizedRequest() {
        val interactionId = args.unauthorizedRequestInteractionId
        interactionId.let {
            val requestToHandle = incomingRequestRepository.getRequest(interactionId) as? WalletUnauthorizedRequest
            if (requestToHandle == null) { // should never happen because
                // the validation first occurs in the initialization of the DAppUnauthorizedLoginViewModel
                sendEvent(Event.TerminateVerification)
                return@let
            } else {
                _state.update { state ->
                    state.copy(
                        walletUnauthorizedRequest = requestToHandle,
                        canNavigateBack = args.canNavigateBack
                    )
                }
            }
        }
    }

    private suspend fun getRequestedEntitiesToVerify() {
        val entitiesForProofWithSignatures = args.entitiesForProofWithSignatures
        val profile = getProfileUseCase()
        val activeAccountsOnCurrentNetwork = profile.activeAccountsOnCurrentNetwork
        _state.update { state ->
            state.copy(
                requestedPersona = entitiesForProofWithSignatures.personaAddress?.let { identityAddress ->
                    profile.activePersonaOnCurrentNetwork(identityAddress)?.asProfileEntity()
                },
                requestedAccounts = activeAccountsOnCurrentNetwork.filter { activeAccount ->
                    activeAccount.address in entitiesForProofWithSignatures.accountAddresses
                }.map { it.asProfileEntity() }
            )
        }
    }

    fun onContinueClick() {
        state.value.walletUnauthorizedRequest?.let { request ->
            viewModelScope.launch {
                val signRequest = request.proofOfOwnershipRequestItem?.challenge?.hex?.let { challengeHex ->
                    SignRequest.SignAuthChallengeRequest(
                        challengeHex = challengeHex,
                        origin = request.metadata.origin,
                        dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
                    )
                }

                signRequest?.let {
                    accessFactorSourcesProxy.getSignatures(
                        accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                            signPurpose = SignPurpose.SignAuth,
                            signRequest = signRequest,
                            signers = state.value.nextEntitiesForProof
                        )
                    ).onSuccess { result ->
                        _state.update { state ->
                            state.copy(signatures = result.signersWithSignatures)
                        }

                        val isPersona = result.signersWithSignatures.keys.first() is ProfileEntity.PersonaEntity
                        if (isPersona && state.value.requestedAccounts.isNotEmpty()) {
                            sendEvent(
                                Event.NavigateToVerifyAccounts(
                                    walletUnauthorizedRequestInteractionId = request.interactionId,
                                    entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                                        accountAddresses = state.value.requestedAccounts.map { it.accountAddress },
                                        signatures = result.signersWithSignatures.mapKeys { it.key.address }
                                    )
                                )
                            )
                        } else {
                            sendEvent(Event.EntitiesVerified)
                        }
                    }
                }
            }
        }
    }

    data class State(
        val walletUnauthorizedRequest: WalletUnauthorizedRequest? = null,
        val requestedPersona: ProfileEntity.PersonaEntity? = null,
        val requestedAccounts: List<ProfileEntity.AccountEntity> = emptyList(),
        val signatures: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap(),
        val canNavigateBack: Boolean = false
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
    }

    sealed interface Event : OneOffEvent {

        data object TerminateVerification : Event

        data object EntitiesVerified : Event

        data class NavigateToVerifyAccounts(
            val walletUnauthorizedRequestInteractionId: String,
            val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
        ) : Event
    }
}
