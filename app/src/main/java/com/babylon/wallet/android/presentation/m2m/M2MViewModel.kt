package com.babylon.wallet.android.presentation.m2m

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.decodeHex
import rdx.works.core.generateX25519KeyPair
import rdx.works.core.generateX25519SharedSecret
import timber.log.Timber
import javax.inject.Inject

@Suppress("UnsafeCallOnNullableType")
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
            args.isValidRequest() -> {
                viewModelScope.launch {
                    rcrRepository.getRequest(args.sessionId!!, args.interactionId!!).mapCatching { walletInteraction ->
                        walletInteraction.toDomainModel(
                            MessageFromDataChannel.RemoteEntityID.RadixMobileConnectRemoteEntityId(args.sessionId)
                        )
                    }.onSuccess { request ->
                        incomingRequestRepository.add(request)
                        sendEvent(Event.Close)
                    }.onFailure {
                        Timber.d(it)
                    }
                }
            }

            args.isValidConnect() -> {
                viewModelScope.launch {
                    _state.update { it.copy(isConnecting = true) }
                    wellKnownDAppDefinitionRepository.getWellDappDefinitions(args.origin!!).onSuccess { dAppDefinitions ->
                        val dappDefinition = dAppDefinitions.dAppDefinitions.firstOrNull()
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
                                x25519PrivateKeyCompressed = HexCoded32Bytes(keyPair.first),
                                callbackPath = dAppDefinitions.callbackPath
                            )
                            _state.update { state -> state.copy(dappLink = dappLink) }
                            dappLinkRepository.saveDappLink(dappLink).onSuccess {
                                sendEvent(
                                    Event.OpenUrl(
                                        Uri.parse(args.origin).buildUpon().apply {
                                            appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_SESSION_ID, args.sessionId)
                                            appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_PUBLIC_KEY, publicKeyHex)
                                            appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_SECRET, secret)
                                            fragment(dAppDefinitions.callbackPath?.replace("#", ""))
                                        }.build().toString()
                                    )
                                )
                            }.onFailure { error ->
                                Timber.d(error)
                            }
                        }
                    }.onFailure {}
                    _state.update { it.copy(isConnecting = false) }
                }
            }

            else -> {}
        }
    }

    sealed class Event : OneOffEvent {
        data class OpenUrl(val url: String) : Event()
        data object Close : Event()
    }
}

data class State(
    val dappLink: DappLink? = null,
    val radixConnectUrl: String? = null,
    val receivedPublicKey: String? = null,
    val isConnecting: Boolean = false
) : UiState
