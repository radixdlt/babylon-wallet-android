package com.babylon.wallet.android.presentation.dapp.authorized.verifyentities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.signing.SignAuthUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel.Event
import com.radixdlt.sargon.SignatureWithPublicKey
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
        state.value.walletAuthorizedRequest?.let { request ->
            viewModelScope.launch {
                setSigningInProgress(true)

                val item = request.proofOfOwnershipRequestItem ?: return@launch

                signAuthUseCase(
                    challenge = item.challenge,
                    entities = state.value.nextEntitiesForProof,
                    metadata = request.metadata
                ).onSuccess { signersWithSignatures ->
                    _state.update { state ->
                        state.copy(signatures = signersWithSignatures)
                    }

                    val isPersona = signersWithSignatures.keys.first() is ProfileEntity.PersonaEntity
                    if (isPersona && state.value.requestedAccounts.isNotEmpty()) {
                        sendEvent(
                            Event.NavigateToVerifyAccounts(
                                walletAuthorizedRequestInteractionId = request.interactionId,
                                entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                                    accountAddresses = state.value.requestedAccounts.map { it.accountAddress },
                                    signatures = signersWithSignatures.mapKeys { it.key.address }
                                )
                            )
                        )
                    } else {
                        sendEvent(Event.EntitiesVerified)
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
        }
    }

    private fun setSigningInProgress(isEnabled: Boolean) = _state.update { it.copy(isSigningInProgress = isEnabled) }

    data class State(
        val walletAuthorizedRequest: WalletAuthorizedRequest? = null,
        val requestedPersona: ProfileEntity.PersonaEntity? = null,
        val requestedAccounts: List<ProfileEntity.AccountEntity> = emptyList(),
        val signatures: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap(),
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
    }

    sealed interface Event : OneOffEvent {

        data object TerminateVerification : Event

        data object EntitiesVerified : Event

        data class NavigateToVerifyAccounts(
            val walletAuthorizedRequestInteractionId: String,
            val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
        ) : Event

        data class AuthorizationFailed(val throwable: RadixWalletException) : Event
    }
}
