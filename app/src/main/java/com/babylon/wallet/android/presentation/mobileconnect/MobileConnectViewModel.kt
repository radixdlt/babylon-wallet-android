package com.babylon.wallet.android.presentation.mobileconnect

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.DappLinkRepository
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.DappDefinition
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.decodeHex
import rdx.works.core.domain.DApp
import rdx.works.core.generateX25519KeyPair
import rdx.works.core.generateX25519SharedSecret
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("UnsafeCallOnNullableType", "LongParameterList")
@HiltViewModel
class MobileConnectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    private val rcrRepository: RcrRepository,
    private val dappLinkRepository: DappLinkRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val getDAppWithResourcesUseCase: GetDAppWithResourcesUseCase
) : StateViewModel<State>(), OneOffEventHandler<MobileConnectViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)
    override fun initialState(): State {
        return State()
    }

    init {
        observeAutoLink()
        viewModelScope.launch {
            val profileInitialized = getProfileUseCase.isInitialized()
            if (!profileInitialized) {
                _state.update {
                    it.copy(isProfileInitialized = false)
                }
                return@launch
            }
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
                    _state.update {
                        it.copy(isLoading = false)
                    }
                    wellKnownDAppDefinitionRepository.getWellKnownDappDefinitions(args.origin!!).mapCatching { dAppDefinitions ->
                        dAppDefinitions.dAppDefinitions.firstOrNull()?.let { dAppDefinition ->
                            getDAppWithResourcesUseCase(dAppDefinition.dAppDefinitionAddress, false).getOrNull()?.dApp?.let { dApp ->
                                val callbackPath = dAppDefinitions.callbackPath ?: error("No callback path found for origin ${args.origin}")
                                _state.update { it.copy(dApp = dApp, dAppDefinition = dAppDefinition, callbackPath = callbackPath) }
                            }
                        }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(uiMessage = UiMessage.ErrorMessage(error))
                        }
                        delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                        sendEvent(Event.Close)
                    }
                    if (_state.value.autoLink && _state.value.canLink &&
                        _state.value.dAppDefinition?.dAppDefinitionAddress?.string != null
                    ) {
                        linkWithDapp(withDelay = true)
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

    private fun observeAutoLink() {
        viewModelScope.launch {
            preferencesManager.mobileConnectAutoLink.collect { autoLink ->
                _state.update {
                    it.copy(autoLink = autoLink)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun linkWithDapp(withDelay: Boolean = false) {
        _state.update {
            it.copy(isLinking = true)
        }
        if (withDelay) {
            delay(2 * 1000L)
        }
        val keyPair = generateX25519KeyPair().getOrNull() ?: error("Failed to generate X25519 key pair")
        val publicKeyHex = keyPair.second
        val receivedPublicKey = args.publicKey!!.decodeHex()
        val secret =
            generateX25519SharedSecret(keyPair.first.decodeHex(), receivedPublicKey).getOrNull()
                ?: error("Failed to generate ecdh curve25519 shared secret")
        val dappLink = DappLink(
            origin = args.origin!!,
            address = _state.value.dAppDefinition!!.dAppDefinitionAddress.string,
            secret = secret,
            sessionId = args.sessionId.orEmpty(),
            x25519PrivateKeyCompressed = keyPair.first,
            callbackPath = _state.value.callbackPath
        )
        dappLinkRepository.saveAsTemporary(dappLink).onSuccess {
            sendEvent(
                Event.OpenUrl(
                    Uri.parse(args.origin).buildUpon().apply {
                        appendPath(dappLink.callbackPath?.replace("/", ""))
                        appendQueryParameter(Constants.RadixMobileConnect.CONNECT_URL_PARAM_SESSION_ID, args.sessionId)
                        appendQueryParameter(
                            Constants.RadixMobileConnect.CONNECT_URL_PARAM_PUBLIC_KEY,
                            publicKeyHex
                        )
                    }.build().toString(),
                    args.browser
                )
            )
            _state.update { state ->
                state.copy(isLinking = false)
            }
        }.onFailure { error ->
            _state.update {
                it.copy(uiMessage = UiMessage.ErrorMessage(error), isLinking = false)
            }
            delay(Constants.SNACKBAR_SHOW_DURATION_MS)
            sendEvent(Event.Close)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onAutoConfirmChange(autoConfirm: Boolean) {
        viewModelScope.launch {
            preferencesManager.autoLinkWithDapps(autoConfirm)
        }
    }

    fun onLinkWithDapp() {
        viewModelScope.launch {
            if (_state.value.canLink) {
                linkWithDapp()
            } else {
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(IllegalStateException("No dAppDefinition found for dApp")),
                        isLinking = false
                    )
                }
            }
        }
    }

    sealed class Event : OneOffEvent {
        data class OpenUrl(val url: String, val browserName: String? = null) : Event()
        data object Close : Event()
    }
}

data class State(
    val dApp: DApp? = null,
    val dAppDefinition: DappDefinition? = null,
    val uiMessage: UiMessage? = null,
    val isProfileInitialized: Boolean = true,
    val isLoading: Boolean = true,
    val linkDelaySeconds: Int = 0,
    val callbackPath: String = "",
    val autoLink: Boolean = false,
    val isLinking: Boolean = false
) : UiState {
    val canLink: Boolean
        get() = dApp != null && dAppDefinition != null
}