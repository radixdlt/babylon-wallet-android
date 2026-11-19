package rdx.works.peerdroid.data

import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.P2pStunServer
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.P2pTurnServer
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.PeerConnectionStatus

@OptIn(ExperimentalCoroutinesApi::class)
class PeerdroidConnectorImplTest {

    private val password = RadixConnectPassword(
        value = Exactly32Bytes.init(ByteArray(32) { index -> index.toByte() }.toBagOfBytes())
    )
    private val connectionId = ConnectionIdHolder(password)
    private val transportProfile = P2pTransportProfile(
        name = "Test",
        signalingServer = "wss://signaling.example.com",
        stun = P2pStunServer(urls = listOf("stun:stun.example.com:19302")),
        turn = P2pTurnServer(urls = emptyList(), username = null, credential = null)
    )

    /** One signaling session the connector opened, or tried to. */
    private class Session(val opens: Boolean) {
        var isOpen = false
        var closeCount = 0
        val messages = MutableSharedFlow<SignalingServerMessage>()
        val client: WebSocketClient = mockk(relaxed = true) {
            coEvery { initSession(any(), any(), any()) } answers {
                if (opens) {
                    isOpen = true
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("no network"))
                }
            }
            every { listenForMessages() } returns messages
            coEvery { closeSession() } answers {
                closeCount++
                isOpen = false
            }
        }
    }

    // Whether each next session can be opened (true once the list is used up).
    private val sessionOutcomes = ArrayDeque<Boolean>()
    private val sessions = mutableListOf<Session>()

    private val peerEvents = MutableSharedFlow<PeerConnectionEvent>()
    private val webRtcManager = mockk<WebRtcManager>(relaxed = true) {
        every { createPeerConnection(any()) } returns peerEvents
        every { getDataChannel() } returns mockk<DataChannel>(relaxed = true) {
            every { state() } returns DataChannel.State.OPEN
            every { label() } returns "dapp"
        }
    }

    private fun TestScope.connector() = PeerdroidConnectorImpl(
        applicationContext = mockk(relaxed = true),
        applicationScope = backgroundScope,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        webSocketClientFactory = { Session(opens = sessionOutcomes.removeFirstOrNull() ?: true).also { sessions += it }.client },
        webRtcManagerFactory = { webRtcManager },
        monotonicMillis = { testScheduler.currentTime }
    )

    private suspend fun PeerdroidConnectorImpl.status() = peerConnectionStatus.first()[connectionId.id]

    private fun TestScope.backAndForth(sut: PeerdroidConnectorImpl) {
        sut.onAppBackground()
        sut.onAppForeground()
        runCurrent()
    }

    // The connector extension connects to the link's session and the peer connection starts negotiating.
    private suspend fun TestScope.startNegotiating(sut: PeerdroidConnectorImpl) {
        runCurrent()
        sessions.last().messages.emit(SignalingServerMessage.RemoteInfo.ClientConnected(remoteClientId = "dapp"))
        runCurrent()
        peerEvents.emit(PeerConnectionEvent.RenegotiationNeeded)
        runCurrent()
        assertEquals(PeerConnectionStatus.CONNECTING, sut.status())
    }

    @Test
    fun `an open link is left alone in the foreground`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, password)
        startNegotiating(sut)
        peerEvents.emit(PeerConnectionEvent.Connected)
        runCurrent()
        assertEquals(PeerConnectionStatus.OPEN, sut.status())

        backAndForth(sut)

        assertEquals(1, sessions.size)
        assertEquals(0, sessions.single().closeCount)
        assertEquals(PeerConnectionStatus.OPEN, sut.status())
    }

    @Test
    fun `a link whose peer connection failed is opened again once in the foreground`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, password)
        startNegotiating(sut)
        peerEvents.emit(PeerConnectionEvent.Connected)
        runCurrent()
        peerEvents.emit(PeerConnectionEvent.Failed(remoteClientId = "dapp"))
        runCurrent()
        assertEquals(PeerConnectionStatus.CLOSED, sut.status())

        backAndForth(sut)

        assertEquals(2, sessions.size)
        assertEquals(1, sessions.first().closeCount)
        assertTrue(sessions.last().isOpen)
    }

    @Test
    fun `a link whose session no connector extension joined is opened again once in the foreground`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, password)
        runCurrent()

        backAndForth(sut)

        assertEquals(2, sessions.size)
        assertEquals(1, sessions.first().closeCount)
    }

    @Test
    fun `a link still negotiating after the limit is opened again in the foreground, a recent one is not`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, password)
        startNegotiating(sut)

        advanceTimeBy(1_000)
        backAndForth(sut)
        assertEquals("a negotiation of 1 s is left alone", 1, sessions.size)

        advanceTimeBy(30_000)
        backAndForth(sut)
        assertEquals(2, sessions.size)
        assertEquals(1, sessions.first().closeCount)
        verify { webRtcManager.close() }
        assertEquals(PeerConnectionStatus.CLOSED, sut.status())
    }

    @Test
    fun `a link whose signaling session could not be opened is connected again in the foreground`() = runTest {
        val sut = connector()
        sessionOutcomes += false

        assertTrue(sut.connectToConnectorExtension(transportProfile, password).isFailure)
        backAndForth(sut)

        assertEquals(2, sessions.size)
        assertTrue(sessions.last().isOpen)
    }

    @Test
    fun `a link that failed to connect is not connected again once every connection was terminated`() = runTest {
        val sut = connector()
        sessionOutcomes += false
        sut.connectToConnectorExtension(transportProfile, password)

        sut.terminateConnectionToConnectorExtension()
        backAndForth(sut)

        assertEquals(1, sessions.size)
    }

    @Test
    fun `a foreground that did not follow a background connects nothing`() = runTest {
        val sut = connector()
        sessionOutcomes += false
        sut.connectToConnectorExtension(transportProfile, password)

        sut.onAppForeground()
        runCurrent()

        assertEquals(1, sessions.size)
    }

    @Test
    fun `a link deleted while the foreground opens it again keeps no session, whichever runs first`() = runTest {
        for (deleteFirst in listOf(false, true)) {
            sessions.clear()
            val sut = connector()
            sut.connectToConnectorExtension(transportProfile, password)
            runCurrent()
            sut.onAppBackground()

            if (deleteFirst) {
                val deletion = launch { sut.deleteConnector(connectionId) }
                sut.onAppForeground()
                runCurrent()
                deletion.join()
            } else {
                sut.onAppForeground()
                sut.deleteConnector(connectionId)
                runCurrent()
            }

            assertFalse("no session stays open ($deleteFirst)", sessions.any { it.isOpen })
            assertNull(sut.status())
        }
    }
}
