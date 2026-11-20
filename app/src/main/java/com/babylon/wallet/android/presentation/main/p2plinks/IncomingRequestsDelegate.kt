package com.babylon.wallet.android.presentation.main.p2plinks

import androidx.lifecycle.Lifecycle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.p2plink.LinkRemovalNotifier
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.RetiredP2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.usecases.VerifyDAppUseCase
import com.babylon.wallet.android.domain.usecases.login.AuthorizeSpecifiedPersonaUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegateWithEvents
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.main.MainViewModel.Event
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.AppLifecycleObserver
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.shortId
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LongParameterList")
class IncomingRequestsDelegate @Inject constructor(
    private val p2PLinksRepository: P2PLinksRepository,
    private val retiredP2PLinksRepository: RetiredP2PLinksRepository,
    private val linkRemovalNotifier: LinkRemovalNotifier,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val peerdroidClient: PeerdroidClient,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val verifyDappUseCase: VerifyDAppUseCase,
    private val authorizeSpecifiedPersonaUseCase: AuthorizeSpecifiedPersonaUseCase,
    private val appLifecycleObserver: AppLifecycleObserver
) : ViewModelDelegateWithEvents<MainViewModel.State, Event>() {

    private var incomingDappRequestsJob: Job? = null
    private var incomingDappRequestErrorsJob: Job? = null
    private var linkRemovalsJob: Job? = null
    private var observeP2PLinksJob: Job? = null
    private var observeRetiredP2PLinksJob: Job? = null
    private var observeAppLifecycleJob: Job? = null

    override fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<MainViewModel.State>,
        oneOffEventHandler: OneOffEventHandler<Event>
    ) {
        super.invoke(scope, state, oneOffEventHandler)
        handleAllIncomingRequests()
        observeAppLifecycle()
        observeP2PLinks()
    }

    fun verifyIncomingRequest(request: DappToWalletInteraction) {
        viewModelScope.launch {
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
    private fun observeP2PLinks() {
        observeP2PLinksJob?.cancel()
        observeP2PLinksJob = getProfileUseCase.state
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
            .flatMapLatest { p2pLinks ->
                getProfileUseCase.flow.map { profile ->
                    p2pLinks to profile.appPreferences.p2pTransportProfiles.current
                }
            }
            .distinctUntilChanged()
            .map { p2pLinksAndP2pTransportProfile ->
                val p2pLinks = p2pLinksAndP2pTransportProfile.first
                Timber.tag(LOG_TAG).i("🤖 connecting the ${p2pLinks.size} linked connectors")

                // All at once: a link waits for nobody else's transports to start (each one's WebRTC takes a moment to
                // reach the signaling server).
                coroutineScope {
                    p2pLinks.asList().forEach { p2PLink ->
                        launch {
                            establishLinkConnection(
                                p2pTransportProfile = p2pLinksAndP2pTransportProfile.second,
                                connectionPassword = p2PLink.connectionPassword
                            )
                        }
                    }
                }
            }
            .onCompletion {
                // The main screen closed.
                terminatePeerdroid(reason = "the observation of the linked connectors ended")
            }
            .launchIn(viewModelScope)
        // Restarted with the links: terminating Peerdroid closes the links of the notices too.
        observeRetiredP2PLinks()
    }

    /**
     * Tells the connector extensions of the links removed here, one notice per link, for as long as it is retired:
     * see [LinkRemovalNotifier].
     */
    private fun observeRetiredP2PLinks() {
        observeRetiredP2PLinksJob?.cancel()
        observeRetiredP2PLinksJob = viewModelScope.launch {
            val notices = mutableMapOf<String, Job>()
            combine(
                retiredP2PLinksRepository.observeRetiredP2PLinks(),
                getProfileUseCase.flow.map { it.appPreferences.p2pTransportProfiles.current }.distinctUntilChanged()
            ) { retiredLinks, p2pTransportProfile -> retiredLinks to p2pTransportProfile }
                .distinctUntilChanged()
                .collect { (retiredLinks, p2pTransportProfile) ->
                    val wanted = retiredLinks.associateBy { it.connectorId }
                    (notices.keys - wanted.keys).forEach { connectorId -> notices.remove(connectorId)?.cancel() }
                    if (wanted.isNotEmpty()) listenForIncomingMessages()
                    wanted.forEach { (connectorId, retiredLink) ->
                        if (notices[connectorId]?.isActive != true) {
                            notices[connectorId] = launch { linkRemovalNotifier.deliver(retiredLink, p2pTransportProfile) }
                        }
                    }
                }
        }
    }

    private suspend fun establishLinkConnection(
        p2pTransportProfile: P2pTransportProfile,
        connectionPassword: RadixConnectPassword
    ) {
        peerdroidClient.connect(p2pTransportProfile, connectionPassword)
            .onSuccess {
                listenForIncomingMessages()
            }
            .onFailure { throwable ->
                Timber.tag(LOG_TAG).e("🤖 failed to connect a linked connector: ${throwable.javaClass.simpleName}")
            }
    }

    /**
     * Listens, once for every link (they all share one stream of messages), to what the connector extensions send.
     */
    private fun listenForIncomingMessages() {
        if (incomingDappRequestsJob != null) return
        Timber.tag(LOG_TAG).d("🤖 listening for incoming requests from dApps")
        incomingDappRequestsJob = viewModelScope.launch {
            peerdroidClient
                .listenForIncomingRequests()
                .cancellable()
                .collect { dappToWalletInteraction ->
                    val requestId = dappToWalletInteraction.interactionId
                    Timber.tag(LOG_TAG).d(
                        "🤖 ${dappToWalletInteraction.remoteEntityId.logId()} verifying incoming interaction=$requestId"
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
        linkRemovalsJob = viewModelScope.launch {
            peerdroidClient
                .listenForLinkRemovals()
                .cancellable()
                .collect { removal ->
                    val removed = linkRemovalNotifier.onLinkRemovedByConnector(removal.remoteConnectorId) ?: return@collect
                    _state.update { it.copy(connectorRemovedByExtension = removed.displayName) }
                }
        }
    }

    /**
     * Tells the transports when the app comes to the foreground and goes to the background, for the whole life of the
     * main screen. Each transport recovers its own connections on the foreground (iroh restarts the sessions it
     * stopped and checks the ones it kept; WebRTC reopens the signaling sessions that may have died): nothing is
     * terminated here, so no session just restarted is cut.
     */
    private fun observeAppLifecycle() {
        observeAppLifecycleJob?.cancel()
        observeAppLifecycleJob = appLifecycleObserver.lifecycleEvents.onEach { event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Timber.tag(LOG_TAG).i("🤖 app in the foreground: the transports check their connections")
                    peerdroidClient.onAppForeground()
                }

                // Requests cannot be approved in the background: the iroh sessions stop after a grace period (no dialing,
                // no keep-alives) and start again on the foreground. WebRTC keeps its connections.
                Lifecycle.Event.ON_STOP -> {
                    Timber.tag(LOG_TAG).i("🤖 app in the background: iroh sessions stop after a grace period")
                    peerdroidClient.onAppBackground()
                }

                else -> Unit
            }
        }.launchIn(viewModelScope)
        // In front of the person or not: said as soon as the recent apps screen opens, since an app closed there is
        // killed some 50 ms after it stops, too soon to say anything then.
        appLifecycleObserver.inFront
            .filterNotNull()
            .onEach { inFront -> peerdroidClient.onAppInFront(inFront) }
            .launchIn(viewModelScope)
    }

    private fun terminatePeerdroid(reason: String) {
        Timber.tag(LOG_TAG).i("🤖 terminating every Radix Connect connection: $reason")
        incomingDappRequestsJob?.cancel()
        incomingDappRequestsJob = null
        incomingDappRequestErrorsJob?.cancel()
        incomingDappRequestErrorsJob = null
        linkRemovalsJob?.cancel()
        linkRemovalsJob = null
        peerdroidClient.terminate()
    }

    private fun reportFailure(error: Throwable) {
        Timber.w(error)
        _state.update {
            it.copy(dappRequestFailure = UiMessage.ErrorMessage(error))
        }
    }

    // The short form of a connector's id (a hash of the link's password): never the full id.
    private fun RemoteEntityID.logId(): String = when (this) {
        is RemoteEntityID.ConnectorId -> "link=${ConnectionIdHolder(id).shortId}"
        is RemoteEntityID.RadixMobileConnectRemoteSession -> "mobile session"
    }

    companion object {

        private val REQUEST_HANDLING_DELAY = 300.milliseconds
        private const val LOG_TAG = "RadixConnect"
    }
}
