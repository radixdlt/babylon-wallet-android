package com.babylon.wallet.android.presentation.main

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.usecases.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.domain.usecases.VerifyDappUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.domain.CheckMnemonicIntegrityUseCase
import rdx.works.profile.domain.CorrectLegacyAccountsDerivationPathSchemeUseCase
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.IsAnyEntityCreatedWithOlympiaUseCase
import rdx.works.profile.domain.p2pLinks
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
class MainViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase,
    private val verifyDappUseCase: VerifyDappUseCase,
    private val appEventBus: AppEventBus,
    getProfileStateUseCase: GetProfileStateUseCase,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
    private val preferencesManager: PreferencesManager,
    private val checkMnemonicIntegrityUseCase: CheckMnemonicIntegrityUseCase,
    private val isAnyEntityCreatedWithOlympiaUseCase: IsAnyEntityCreatedWithOlympiaUseCase,
    private val correctLegacyAccountsDerivationPathSchemeUseCase: CorrectLegacyAccountsDerivationPathSchemeUseCase
) : StateViewModel<MainUiState>(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    private var verifyingDappRequestJob: Job? = null
    private var incomingDappRequestsJob: Job? = null
    private var incomingDappRequestErrorsJob: Job? = null
    private var countdownJob: Job? = null

    val observeP2PLinks = getProfileUseCase
        .p2pLinks
        .map { p2pLinks ->
            Timber.d("found ${p2pLinks.size} p2p links")
            p2pLinks.forEach { p2PLink ->
                establishLinkConnection(connectionPassword = p2PLink.connectionPassword)
            }
        }
        .onCompletion {
            terminatePeerdroid()
        }
        .shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(PEERDROID_STOP_TIMEOUT)
        )

    val statusEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.Status>()

    val isDevBannerVisible = getProfileStateUseCase().map { profileState ->
        when (profileState) {
            is ProfileState.Restored -> {
                profileState.profile.currentGateway.network.id != Radix.Gateway.mainnet.network.id
            }

            else -> false
        }
    }

    val appNotSecureEvent = appEventBus.events.filterIsInstance<AppEvent.AppNotSecure>()
    val babylonMnemonicNeedsRecoveryEvent = appEventBus.events.filterIsInstance<AppEvent.BabylonFactorSourceNeedsRecovery>()

    init {
        viewModelScope.launch {
            combine(
                getProfileStateUseCase(),
                preferencesManager.isDeviceRootedDialogShown
            ) { profileState, isDeviceRootedDialogShown ->
                _state.update {
                    MainUiState(
                        initialAppState = AppState.from(
                            profileState = profileState
                        ),
                        showDeviceRootedWarning = deviceCapabilityHelper.isDeviceRooted() && !isDeviceRootedDialogShown
                    )
                }
            }.collect()
        }
        handleAllIncomingRequests()
    }

    override fun initialState(): MainUiState {
        return MainUiState()
    }

    fun onHighPriorityScreen() = viewModelScope.launch {
        incomingRequestRepository.pauseIncomingRequests()
    }

    fun onLowPriorityScreen() = viewModelScope.launch {
        incomingRequestRepository.resumeIncomingRequests()
    }

    private suspend fun establishLinkConnection(connectionPassword: String) {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = connectionPassword
        )
        if (encryptionKey != null) {
            peerdroidClient.connect(encryptionKey = encryptionKey)
                .onSuccess {
                    if (incomingDappRequestsJob == null) {
                        Timber.d("\uD83E\uDD16 Listen for incoming requests from dapps")
                        // We must run this only once
                        // otherwise for each new link connection
                        // we create a new job to collect messages from the same stream (messagesFromRemoteClients).
                        // I think this can be improved.
                        incomingDappRequestsJob = viewModelScope.launch {
                            peerdroidClient
                                .listenForIncomingRequests()
                                .cancellable()
                                .collect { incomingRequest ->
                                    val remoteConnectorId = incomingRequest.remoteConnectorId
                                    val requestId = incomingRequest.id
                                    Timber.d(
                                        "\uD83E\uDD16 wallet received incoming request from " +
                                            "remote connector $remoteConnectorId with id $requestId"
                                    )
                                    verifyIncomingRequest(incomingRequest)
                                }
                        }
                        incomingDappRequestErrorsJob = viewModelScope.launch {
                            peerdroidClient
                                .listenForIncomingRequestErrors()
                                .cancellable()
                                .collect {
                                    _state.update { state ->
                                        state.copy(dappRequestFailure = RadixWalletException.DappRequestException.InvalidRequestChallenge)
                                    }
                                }
                        }
                    }
                }
                .onFailure { throwable ->
                    Timber.e("\uD83E\uDD16 Failed to establish link connection: ${throwable.message}")
                }
        }
    }

    /**
     * Listens for incoming requests made either from connected dApps, or the app itself.
     */
    private fun handleAllIncomingRequests() {
        viewModelScope.launch {
            incomingRequestRepository.currentRequestToHandle.collect { request ->
                if (request.metadata.isInternal) {
                    sendEvent(MainEvent.IncomingRequestEvent(request))
                } else {
                    delay(REQUEST_HANDLING_DELAY)
                    authorizeSpecifiedPersonaUseCase.invoke(request).onSuccess { dAppData ->
                        appEventBus.sendEvent(
                            AppEvent.Status.DappInteraction(
                                requestId = dAppData.requestId,
                                dAppName = dAppData.name
                            )
                        )
                    }.onFailure { exception ->
                        (exception as? RadixWalletException.DappRequestException)?.let { dappRequestFailure ->
                            when (dappRequestFailure) {
                                RadixWalletException.DappRequestException.InvalidPersona,
                                RadixWalletException.DappRequestException.InvalidRequest -> {
                                    incomingRequestRepository.requestHandled(request.id)
                                    _state.update { state ->
                                        state.copy(dappRequestFailure = dappRequestFailure)
                                    }
                                }

                                else -> {
                                    sendEvent(MainEvent.IncomingRequestEvent(request))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun verifyIncomingRequest(request: IncomingRequest) {
        verifyingDappRequestJob = viewModelScope.launch {
            verifyDappUseCase(request).onSuccess { verified ->
                if (verified) {
                    incomingRequestRepository.add(request)
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(dappRequestFailure = RadixWalletException.DappRequestException.InvalidRequest)
                }
            }
        }
    }

    private fun terminatePeerdroid() {
        Timber.d("\uD83E\uDD16 Peerdroid is terminating")
        incomingDappRequestsJob?.cancel()
        incomingDappRequestsJob = null
        verifyingDappRequestJob?.cancel()
        verifyingDappRequestJob = null
        incomingDappRequestErrorsJob?.cancel()
        incomingDappRequestErrorsJob = null
        peerdroidClient.terminate()
        incomingRequestRepository.removeAll()
    }

    fun onInvalidRequestMessageShown() {
        _state.update { it.copy(dappRequestFailure = null) }
    }

    fun clearOlympiaError() {
        _state.update { it.copy(olympiaErrorState = OlympiaErrorState.None) }
    }

    fun onAppToForeground() {
        viewModelScope.launch {
            checkMnemonicIntegrityUseCase()
            val deviceNotSecure = deviceCapabilityHelper.isDeviceSecure().not()
            if (deviceNotSecure) {
                appEventBus.sendEvent(AppEvent.AppNotSecure, delayMs = 500L)
            } else {
                correctLegacyAccountsDerivationPathSchemeUseCase()
                val entitiesCreatedWithOlympiaLegacyFactorSource = isAnyEntityCreatedWithOlympiaUseCase()
                if (entitiesCreatedWithOlympiaLegacyFactorSource) {
                    _state.update { state ->
                        state.copy(olympiaErrorState = OlympiaErrorState.Countdown())
                    }
                    countdownJob?.cancel()
                    countdownJob = startOlympiaErrorCountdown()
                    return@launch
                }
                checkMnemonicIntegrityUseCase.babylonMnemonicNeedsRecovery()?.let { factorSourceId ->
                    appEventBus.sendEvent(AppEvent.BabylonFactorSourceNeedsRecovery(factorSourceId), delayMs = 500L)
                }
            }
        }
    }

    private fun startOlympiaErrorCountdown(): Job {
        return viewModelScope.launch {
            var errorState = state.value.olympiaErrorState
            while (isActive && errorState is OlympiaErrorState.Countdown) {
                delay(TICK_MS)
                val newState = if (errorState.secondsLeft - 1 <= 0) {
                    OlympiaErrorState.CanDismiss
                } else {
                    OlympiaErrorState.Countdown(errorState.secondsLeft - 1)
                }
                errorState = newState
                _state.update { it.copy(olympiaErrorState = newState) }
            }
        }
    }

    companion object {
        private val PEERDROID_STOP_TIMEOUT = 60.seconds
        private const val REQUEST_HANDLING_DELAY = 300L
        private const val TICK_MS = 1000L
    }
}

sealed class MainEvent : OneOffEvent {
    data class IncomingRequestEvent(val request: IncomingRequest) : MainEvent()
}

data class MainUiState(
    val initialAppState: AppState = AppState.Loading,
    val showDeviceRootedWarning: Boolean = false,
    val dappRequestFailure: RadixWalletException.DappRequestException? = null,
    val olympiaErrorState: OlympiaErrorState = OlympiaErrorState.None
) : UiState

sealed interface OlympiaErrorState {
    data class Countdown(val secondsLeft: Int = 30) : OlympiaErrorState
    data object CanDismiss : OlympiaErrorState
    data object None : OlympiaErrorState
}

sealed interface AppState {
    data object OnBoarding : AppState
    data object Wallet : AppState
    data object IncompatibleProfile : AppState
    data object Loading : AppState

    companion object {
        fun from(
            profileState: ProfileState
        ) = when (profileState) {
            is ProfileState.Incompatible -> IncompatibleProfile
            is ProfileState.Restored -> if (profileState.hasMainnet()) {
                Wallet
            } else {
                OnBoarding
            }

            is ProfileState.None -> OnBoarding
            is ProfileState.NotInitialised -> OnBoarding
        }
    }
}
