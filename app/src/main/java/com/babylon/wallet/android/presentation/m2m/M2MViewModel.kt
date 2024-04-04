package com.babylon.wallet.android.presentation.m2m

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.decodeHex
import rdx.works.core.generateX25519KeyPair
import rdx.works.core.generateX25519SharedSecret
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class M2MViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val rcrRepository: RcrRepository,
    private val dappLinkRepository: DappLinkRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<State>(), OneOffEventHandler<M2MViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = M2MArgs(savedStateHandle)
    override fun initialState(): State {
        return State(receivedPublicKey = args.publicKey)
    }

    init {
        when {
            args.interactionId != null && args.sessionId != null-> {
                viewModelScope.launch {
                    rcrRepository.getRequest(args.sessionId, args.interactionId).mapCatching { walletInteraction ->
                        val domainModel = walletInteraction.toDomainModel("")
                        incomingRequestRepository.add(domainModel)
                    }
                }
            }

            else -> {
                args.origin?.let { origin ->
                    viewModelScope.launch {
                        _state.update { it.copy(loadingRadixConnectUrl = true) }
                        wellKnownDAppDefinitionRepository.getWellDappDefinitions(origin).onSuccess { dAppDefinitions ->
                            val dappDefinition = dAppDefinitions.dAppDefinitions.firstOrNull()
                            delay(500)
                            dappDefinition?.let {
                                val keyPair = generateX25519KeyPair().getOrNull() ?: return@let
                                val publicKeyHex = keyPair.second
                                val receivedPublicKey = args.publicKey!!.decodeHex()
                                val secret =
                                    generateX25519SharedSecret(keyPair.first.decodeHex(), receivedPublicKey).getOrNull() ?: return@let
                                val dappLink = DappLink(
                                    origin = args.origin,
                                    dAppDefinitionAddress = dappDefinition.dAppDefinitionAddress,
                                    secret = HexCoded32Bytes(secret),
                                    sessionId = args.sessionId.orEmpty(),
                                    x25519PrivateKeyCompressed = HexCoded32Bytes(keyPair.first)
                                )
                                rcrRepository.sendTest("abc", "abc")
                                _state.update { it.copy(dappLink = dappLink) }
                                dappLinkRepository.saveDappLink(dappLink).onSuccess {
                                    sendEvent(Event.OpenUrl("$origin?sessionId=${args.sessionId}&publicKey=$publicKeyHex&secret=$secret${dAppDefinitions.callbackPath}"))
                                }.onFailure {
                                    Timber.d(it)
                                }
                            }
                        }.onFailure {}
                        _state.update { it.copy(loadingRadixConnectUrl = false) }
                    }
                }
            }
        }
    }

    sealed class Event : OneOffEvent {
        data class OpenUrl(val url: String) : Event()
    }
}

data class State(
    val dappLink: DappLink? = null,
    val radixConnectUrl: String? = null,
    val receivedPublicKey: String? = null,
    val loadingRadixConnectUrl: Boolean = false
) : UiState