package com.babylon.wallet.android.presentation.mobileconnect

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
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

@Suppress("UnsafeCallOnNullableType")
@HiltViewModel
class MobileConnectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val rcrRepository: RcrRepository,
    private val dappLinkRepository: DappLinkRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<State>(), OneOffEventHandler<MobileConnectViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)
    override fun initialState(): State {
        return State()
    }

    init {
        when {
            args.isValidRequest() -> {
                viewModelScope.launch {
                    rcrRepository.getRequest(args.sessionId!!, args.interactionId!!).mapCatching { walletInteraction ->
                        walletInteraction.toDomainModel(
                            IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession(args.sessionId)
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
                    wellKnownDAppDefinitionRepository.getWellDappDefinitions(args.origin!!).mapCatching { dAppDefinitions ->
                        dAppDefinitions.dAppDefinitions.firstOrNull()?.let { dAppDefinition ->
                            val keyPair = generateX25519KeyPair().getOrNull() ?: error("Failed to generate X25519 key pair")
                            val publicKeyHex = keyPair.second
                            val receivedPublicKey = args.publicKey!!.decodeHex()
                            val secret =
                                generateX25519SharedSecret(keyPair.first.decodeHex(), receivedPublicKey).getOrNull()
                                    ?: error("Failed to generate ecdh curve25519 shared secret")
                            val dappLink = DappLink(
                                origin = args.origin,
                                dAppDefinitionAddress = dAppDefinition.dAppDefinitionAddress,
                                secret = HexCoded32Bytes(secret),
                                sessionId = args.sessionId.orEmpty(),
                                x25519PrivateKeyCompressed = HexCoded32Bytes(keyPair.first),
                                callbackPath = dAppDefinitions.callbackPath
                            )
                            dappLinkRepository.saveDappLink(dappLink).getOrThrow() to publicKeyHex
                        } ?: error("No dApp definition found for origin ${args.origin}")
                    }.onSuccess { dappLinkToPublicKey ->
                        sendEvent(
                            Event.OpenUrl(
                                Uri.parse(args.origin).buildUpon().apply {
                                    appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_SESSION_ID, args.sessionId)
                                    appendQueryParameter(
                                        Constants.RadixMobileConnect.CONNECT_URL_PARAM_PUBLIC_KEY,
                                        dappLinkToPublicKey.second
                                    )
                                    appendQueryParameter(
                                        Constants.RadixMobileConnect.CONNECT_URL_PARAM_SECRET,
                                        dappLinkToPublicKey.first.secret.value
                                    )
                                    fragment(dappLinkToPublicKey.first.callbackPath?.replace("#", ""))
                                }.build().toString()
                            )
                        )
                    }.onFailure { error ->
                        _state.update {
                            it.copy(uiMessage = UiMessage.ErrorMessage(error))
                        }
                        delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                        sendEvent(Event.Close)
                    }
                }
            }

            else -> {
                _state.update {
                    it.copy(uiMessage = UiMessage.ErrorMessage(IllegalStateException("Invalid dApp request")))
                }
                viewModelScope.launch {
                    delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                    sendEvent(Event.Close)
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    sealed class Event : OneOffEvent {
        data class OpenUrl(val url: String) : Event()
        data object Close : Event()
    }
}

data class State(
    val isConnecting: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState
