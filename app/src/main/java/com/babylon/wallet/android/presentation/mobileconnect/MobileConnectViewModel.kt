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
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.decodeHex
import rdx.works.core.domain.DApp
import rdx.works.core.domain.ProfileState
import rdx.works.core.generateX25519KeyPair
import rdx.works.core.generateX25519SharedSecret
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileStateUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("UnsafeCallOnNullableType")
@HiltViewModel
class MobileConnectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val rcrRepository: RcrRepository,
    private val dappLinkRepository: DappLinkRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val profileStateUseCase: GetProfileStateUseCase,
    private val preferencesManager: PreferencesManager,
    private val getDAppWithResourcesUseCase: GetDAppWithResourcesUseCase
) : StateViewModel<State>(), OneOffEventHandler<MobileConnectViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)
    override fun initialState(): State {
        return State()
    }

    init {
        viewModelScope.launch {
            val profileState = profileStateUseCase().firstOrNull()
            val connectDelaySeconds = preferencesManager.mobileConnectDelaySeconds.firstOrNull() ?: 0
            val profileInitialized = profileState is ProfileState.Restored && profileState.hasNetworks()
            if (!profileInitialized) {
                _state.update {
                    it.copy(isProfileInitialized = false)
                }
                return@launch
            }
            when {
                args.isValidRequest() -> {
                    _state.update {
                        it.copy(requestType = State.RequestType.REQUEST)
                    }
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
                    _state.update {
                        it.copy(requestType = State.RequestType.CONNECT)
                    }
                    viewModelScope.launch {
                        wellKnownDAppDefinitionRepository.getWellKnownDappDefinitions(args.origin!!).mapCatching { dAppDefinitions ->
                            dAppDefinitions.dAppDefinitions.firstOrNull()?.let { dAppDefinition ->
                                getDAppWithResourcesUseCase(dAppDefinition.dAppDefinitionAddress, false).getOrNull()?.dApp?.let { dApp ->
                                    _state.update { it.copy(dApp = dApp) }
                                }
                                delay(connectDelaySeconds * 1000L)
                                val keyPair = generateX25519KeyPair().getOrNull() ?: error("Failed to generate X25519 key pair")
                                val publicKeyHex = keyPair.second
                                val receivedPublicKey = args.publicKey!!.decodeHex()
                                val secret =
                                    generateX25519SharedSecret(keyPair.first.decodeHex(), receivedPublicKey).getOrNull()
                                        ?: error("Failed to generate ecdh curve25519 shared secret")
                                val dappLink = DappLink(
                                    origin = args.origin,
                                    address = dAppDefinition.dAppDefinitionAddress.string,
                                    secret = secret,
                                    sessionId = args.sessionId.orEmpty(),
                                    x25519PrivateKeyCompressed = keyPair.first,
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
                                            dappLinkToPublicKey.first.secret
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
                    viewModelScope.launch {
                        sendEvent(Event.Close)
                    }
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
    val dApp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val isProfileInitialized: Boolean = true,
    val requestType: RequestType? = null
) : UiState {
    enum class RequestType {
        CONNECT, REQUEST
    }
}
