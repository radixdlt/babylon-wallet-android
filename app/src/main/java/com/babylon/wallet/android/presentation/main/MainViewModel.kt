package com.babylon.wallet.android.presentation.main

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.usecases.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.domain.usecases.VerifyDAppUseCase
import com.babylon.wallet.android.domain.usecases.p2plink.ObserveAccountsAndSyncWithConnectorExtensionUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.RadixConnectPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.CheckEntitiesCreatedWithOlympiaUseCase
import rdx.works.profile.domain.CheckMnemonicIntegrityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
class MainViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    p2PLinksRepository: P2PLinksRepository,
    private val peerdroidClient: PeerdroidClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase,
    private val verifyDappUseCase: VerifyDAppUseCase,
    private val appEventBus: AppEventBus,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
    private val preferencesManager: PreferencesManager,
    private val checkMnemonicIntegrityUseCase: CheckMnemonicIntegrityUseCase,
    private val checkEntitiesCreatedWithOlympiaUseCase: CheckEntitiesCreatedWithOlympiaUseCase,
    private val observeAccountsAndSyncWithConnectorExtensionUseCase: ObserveAccountsAndSyncWithConnectorExtensionUseCase
) : StateViewModel<MainUiState>(), OneOffEventHandler<MainEvent> by OneOffEventHandlerImpl() {

    private var verifyingDappRequestJob: Job? = null
    private var incomingDappRequestsJob: Job? = null
    private var incomingDappRequestErrorsJob: Job? = null
    private var countdownJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val observeP2PLinks = getProfileUseCase.observeIsInitialized()
        .filter { isInitialized -> isInitialized }
        .flatMapLatest { p2PLinksRepository.observeP2PLinks() }
        .map { p2pLinks ->
            if (!getProfileUseCase.isInitialized()) {
                return@map
            }

            Timber.d("found ${p2pLinks.size} p2p links")
            p2pLinks.asList().forEach { p2PLink ->
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

    val accessFactorSourcesEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.AccessFactorSources>()

    val isDevBannerVisible = getProfileUseCase.state.map { profileState ->
        when (profileState) {
            is ProfileState.Restored -> {
                profileState.profile.currentGateway.network.id != NetworkId.MAINNET
            }

            else -> false
        }
    }

    val appNotSecureEvent = appEventBus.events.filterIsInstance<AppEvent.AppNotSecure>()
    val secureFolderWarning = appEventBus.events.filterIsInstance<AppEvent.SecureFolderWarning>()

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.state,
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

        viewModelScope.launch {
            observeAccountsAndSyncWithConnectorExtensionUseCase()
        }
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

    private suspend fun establishLinkConnection(connectionPassword: RadixConnectPassword) {
        peerdroidClient.connect(connectionPassword)
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
        _state.update { it.copy(olympiaErrorState = null) }
    }

    fun onAppToForeground() {
        viewModelScope.launch {
            checkMnemonicIntegrityUseCase()
            val deviceNotSecure = deviceCapabilityHelper.isDeviceSecure().not()
            if (deviceNotSecure) {
                appEventBus.sendEvent(AppEvent.AppNotSecure, delayMs = 500L)
            } else {
                val checkResult = checkEntitiesCreatedWithOlympiaUseCase()
                if (checkResult.isAnyEntityCreatedWithOlympia) {
                    _state.update { state ->
                        state.copy(
                            olympiaErrorState = OlympiaErrorState(
                                affectedAccounts = checkResult.affectedAccounts,
                                affectedPersonas = checkResult.affectedPersonas
                            )
                        )
                    }
                    countdownJob?.cancel()
                    countdownJob = startOlympiaErrorCountdown()
                    return@launch
                }
            }
        }
    }

    private fun startOlympiaErrorCountdown(): Job {
        return viewModelScope.launch {
            while (isActive && state.value.olympiaErrorState?.isCountdownActive == true) {
                delay(TICK_MS)
                _state.update { state ->
                    state.copy(
                        olympiaErrorState = state.olympiaErrorState?.copy(
                            secondsLeft = state.olympiaErrorState.secondsLeft - 1
                        )
                    )
                }
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
    val olympiaErrorState: OlympiaErrorState? = null
) : UiState

data class OlympiaErrorState(
    val secondsLeft: Int = 30,
    val affectedAccounts: List<Account>,
    val affectedPersonas: List<Persona>
) {
    val isCountdownActive: Boolean
        get() = secondsLeft > 0
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
            is ProfileState.Restored -> if (profileState.hasNetworks()) {
                Wallet
            } else {
                OnBoarding
            }

            is ProfileState.None -> OnBoarding
            is ProfileState.NotInitialised -> OnBoarding
        }
    }
}
