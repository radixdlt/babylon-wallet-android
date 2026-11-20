package com.babylon.wallet.android.presentation.main.p2plinks

import androidx.lifecycle.Lifecycle
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.p2plink.RetiredP2PLinksRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingMessage
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.utils.AppLifecycleObserver
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.samples.sample
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.profile.domain.GetProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class IncomingRequestsDelegateTest {

    private val peerdroidClient = FakePeerdroidClient()
    private val lifecycleObserver = object : AppLifecycleObserver {
        override val lifecycleEvents = MutableSharedFlow<Lifecycle.Event>()
        override val inFront = MutableStateFlow<Boolean?>(null)
        override fun onTopResumedActivityChanged(activity: Any, isTop: Boolean) {
            inFront.value = isTop
        }
    }
    // Like the real ones, these flows never complete: completing would terminate the connections.
    private val p2pLinksRepository = mockk<P2PLinksRepository> {
        every { observeP2PLinks() } returns MutableStateFlow(P2pLinks(P2pLink.sample()))
    }
    private val retiredP2PLinksRepository = mockk<RetiredP2PLinksRepository> {
        every { observeRetiredP2PLinks() } returns MutableStateFlow(emptyList())
    }
    private val incomingRequestRepository = mockk<IncomingRequestRepository> {
        every { currentRequestToHandle } returns emptyFlow()
    }
    private val getProfileUseCase = mockk<GetProfileUseCase> {
        every { state } returns MutableStateFlow(ProfileState.Loaded(Profile.sample()))
        every { flow } returns MutableStateFlow(Profile.sample())
    }

    private fun TestScope.startDelegate() {
        val delegate = IncomingRequestsDelegate(
            p2PLinksRepository = p2pLinksRepository,
            retiredP2PLinksRepository = retiredP2PLinksRepository,
            linkRemovalNotifier = mockk(relaxed = true),
            incomingRequestRepository = incomingRequestRepository,
            peerdroidClient = peerdroidClient,
            appEventBus = mockk(relaxed = true),
            getProfileUseCase = getProfileUseCase,
            verifyDappUseCase = mockk(relaxed = true),
            authorizeSpecifiedPersonaUseCase = mockk(relaxed = true),
            appLifecycleObserver = lifecycleObserver
        )
        delegate(
            scope = backgroundScope,
            state = MutableStateFlow(MainViewModel.State(isDeviceSecure = true)),
            oneOffEventHandler = OneOffEventHandlerImpl()
        )
        runCurrent()
    }

    @Test
    fun `coming to the foreground checks the iroh connections even when a connection is reported active`() = runTest {
        peerdroidClient.hasConnection.value = true
        startDelegate()
        assertEquals(1, peerdroidClient.connectCount)

        lifecycleObserver.lifecycleEvents.emit(Lifecycle.Event.ON_START)
        runCurrent()

        assertEquals(1, peerdroidClient.appForegroundCount)
        // Nothing else changes: the links are not reconnected.
        assertEquals(1, peerdroidClient.connectCount)
        assertEquals(0, peerdroidClient.terminateCount)
    }

    @Test
    fun `coming to the foreground without any connection leaves the recovery to the transports, cutting nothing`() = runTest {
        peerdroidClient.hasConnection.value = false
        startDelegate()

        lifecycleObserver.lifecycleEvents.emit(Lifecycle.Event.ON_START)
        runCurrent()

        assertEquals(1, peerdroidClient.appForegroundCount)
        // Each transport recovers its own connections: terminating them would cut the iroh sessions just restarted.
        assertEquals(0, peerdroidClient.terminateCount)
        assertEquals(1, peerdroidClient.connectCount)
    }

    @Test
    fun `leaving the front and coming back is told to the transports, before the background`() = runTest {
        startDelegate()
        // Nothing is told before it is known.
        assertEquals(emptyList<Boolean>(), peerdroidClient.inFrontToldValues)

        // The recent apps screen opened: the app is still resumed, but no longer in front.
        lifecycleObserver.inFront.value = false
        runCurrent()
        assertEquals(listOf(false), peerdroidClient.inFrontToldValues)
        assertEquals(0, peerdroidClient.appBackgroundCount)

        lifecycleObserver.inFront.value = true
        runCurrent()
        assertEquals(listOf(false, true), peerdroidClient.inFrontToldValues)
        assertEquals(0, peerdroidClient.appForegroundCount)
    }

    @Test
    fun `going to the background asks for the iroh sessions to stop, and nothing else`() = runTest {
        startDelegate()

        lifecycleObserver.lifecycleEvents.emit(Lifecycle.Event.ON_STOP)
        runCurrent()

        assertEquals(1, peerdroidClient.appBackgroundCount)
        assertEquals(0, peerdroidClient.appForegroundCount)
        // WebRTC is left as it is: nothing is terminated or reconnected.
        assertEquals(0, peerdroidClient.terminateCount)
        assertEquals(1, peerdroidClient.connectCount)
    }

    private class FakePeerdroidClient : PeerdroidClient {
        val hasConnection = MutableStateFlow(true)
        var connectCount = 0
        var terminateCount = 0
        var appForegroundCount = 0
        var appBackgroundCount = 0

        override val hasAtLeastOneConnection: Flow<Boolean> = hasConnection
        override val openConnectionIds: Flow<Set<String>> = flowOf(emptySet())
        override fun isConnectionOpen(remoteConnectorId: String): Flow<Boolean> = flowOf(false)

        override suspend fun connect(p2pTransportProfile: P2pTransportProfile, connectionPassword: RadixConnectPassword): Result<Unit> {
            connectCount++
            return Result.success(Unit)
        }

        override suspend fun sendMessage(remoteConnectorId: String, message: String): Result<Unit> = Result.success(Unit)
        override suspend fun sendMessage(message: String): Result<Unit> = Result.success(Unit)
        override fun listenForIncomingRequests(): Flow<DappToWalletInteraction> = emptyFlow()
        override fun listenForLedgerResponses(): Flow<LedgerResponse> = emptyFlow()
        override fun listenForIncomingRequestErrors(): Flow<IncomingMessage.Error> = emptyFlow()
        override fun listenForLinkRemovals(): Flow<IncomingMessage.LinkRemoved> = emptyFlow()
        override suspend fun deleteLink(connectionPassword: RadixConnectPassword) = Unit

        override fun terminate() {
            terminateCount++
        }

        override fun onAppForeground() {
            appForegroundCount++
        }

        override fun onAppBackground() {
            appBackgroundCount++
        }

        val inFrontToldValues = mutableListOf<Boolean>()

        override fun onAppInFront(inFront: Boolean) {
            inFrontToldValues += inFront
        }
    }
}
