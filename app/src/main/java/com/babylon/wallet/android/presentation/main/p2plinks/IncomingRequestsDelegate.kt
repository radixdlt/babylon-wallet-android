package com.babylon.wallet.android.presentation.main.p2plinks

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.VerifyDAppUseCase
import com.babylon.wallet.android.domain.usecases.login.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegateWithEvents
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.main.MainViewModel.Event
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
class IncomingRequestsDelegate @Inject constructor(
    private val p2PLinksRepository: P2PLinksRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val peerdroidClient: PeerdroidClient,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val verifyDappUseCase: VerifyDAppUseCase,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase
) : ViewModelDelegateWithEvents<MainViewModel.State, Event>() {

    private var verifyingDappRequestJob: Job? = null
    private var incomingDappRequestsJob: Job? = null
    private var incomingDappRequestErrorsJob: Job? = null

    val observeP2PLinks by lazy { observeP2PLinks() }

    override fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<MainViewModel.State>,
        oneOffEventHandler: OneOffEventHandler<Event>
    ) {
        super.invoke(scope, state, oneOffEventHandler)
        handleAllIncomingRequests()
    }

    fun verifyIncomingRequest(request: DappToWalletInteraction) {
        verifyingDappRequestJob = viewModelScope.launch {
            verifyDappUseCase(request).onSuccess { verified ->
                if (verified) {
                    incomingRequestRepository.add(request)
                }
            }.onFailure { error ->
                if (error is RadixWalletException.DappRequestException) {
                    reportFailure(error)
                } else {
                    reportFailure(RadixWalletException.DappRequestException.InvalidRequest)
                }
            }
        }
    }

    /**
     * Listens for incoming requests made either from connected dApps, or the app itself.
     */
    private fun handleAllIncomingRequests() {
        viewModelScope.launch {
            incomingRequestRepository.currentRequestToHandle.collect { request ->
                if (request.metadata.isInternal || request.isMobileConnectRequest) {
                    sendEvent(Event.IncomingRequestEvent(request))
                } else {
                    delay(REQUEST_HANDLING_DELAY)
                    authorizeSpecifiedPersonaUseCase.invoke(request).onSuccess { dAppData ->
                        appEventBus.sendEvent(
                            AppEvent.Status.DappInteraction(
                                requestId = dAppData.interactionId,
                                dAppName = dAppData.name
                            )
                        )
                    }.onFailure { exception ->
                        (exception as? RadixWalletException.DappRequestException)?.let { dappRequestFailure ->
                            when (dappRequestFailure) {
                                RadixWalletException.DappRequestException.InvalidPersona,
                                RadixWalletException.DappRequestException.InvalidRequest -> {
                                    incomingRequestRepository.requestHandled(request.interactionId)
                                    reportFailure(dappRequestFailure)
                                }

                                else -> {
                                    sendEvent(Event.IncomingRequestEvent(request))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeP2PLinks(): SharedFlow<Unit> = getProfileUseCase.state
        .flatMapLatest { profileState ->
            /**
             * Observe p2p links after the profile has been restored,
             * otherwise skip the first value and observe only the upcoming changes.
             * This ensures the p2p links connection is not being unnecessarily established,
             * but is being established upon request (e.g when a new p2p link has been added),
             * even if the profile has not yet been restored
             */
            when (profileState) {
                is ProfileState.Loaded -> p2PLinksRepository.observeP2PLinks()
                else -> p2PLinksRepository.observeP2PLinks().drop(1)
            }
        }
        .map { p2pLinks ->
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
                            .collect { dappToWalletInteraction ->
                                val remoteConnectorId = dappToWalletInteraction.remoteEntityId
                                val requestId = dappToWalletInteraction.interactionId
                                Timber.d(
                                    "\uD83E\uDD16 wallet received incoming request from " +
                                        "remote connector $remoteConnectorId with id $requestId"
                                )
                                verifyIncomingRequest(dappToWalletInteraction)
                            }
                    }
                    incomingDappRequestErrorsJob = viewModelScope.launch {
                        peerdroidClient
                            .listenForIncomingRequestErrors()
                            .cancellable()
                            .collect { error ->
                                reportFailure(error.exception)
                            }
                    }
                }
            }
            .onFailure { throwable ->
                Timber.e("\uD83E\uDD16 Failed to establish link connection: ${throwable.message}")
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

    private fun reportFailure(error: Throwable) {
        Timber.w(error)
        _state.update {
            it.copy(dappRequestFailure = UiMessage.ErrorMessage(error))
        }
    }

    companion object {

        private val PEERDROID_STOP_TIMEOUT = 60.seconds
        private val REQUEST_HANDLING_DELAY = 300.milliseconds
    }
}
