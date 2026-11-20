package rdx.works.peerdroid.data

import com.radixdlt.sargon.extensions.hex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.data.fakes.FakePeerdroidConnector
import rdx.works.peerdroid.data.fakes.FakeTransportSettings
import rdx.works.peerdroid.data.fakes.RecordingTree
import rdx.works.peerdroid.data.fakes.password
import rdx.works.peerdroid.data.fakes.transportProfile
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.LinkEvent
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class CompositePeerdroidConnectorTest {

    @get:Rule
    val logs = RecordingTree()

    private val webRtc = FakePeerdroidConnector()
    private val iroh = FakePeerdroidConnector()
    private val settings = FakeTransportSettings()

    private val passwordA = password(seed = 1)
    private val passwordB = password(seed = 2)
    private val idA = ConnectionIdHolder(passwordA).id
    private val idB = ConnectionIdHolder(passwordB).id

    private fun TestScope.composite(
        transport: RadixConnectTransport,
        irohCapable: Set<String> = emptySet()
    ): CompositePeerdroidConnector {
        settings.transportState.value = transport
        settings.irohCapableState.value = irohCapable
        return CompositePeerdroidConnector(
            connectors = mapOf(PeerdroidTransport.WEBRTC to webRtc, PeerdroidTransport.IROH to iroh),
            settings = settings,
            applicationScope = backgroundScope,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            monotonicMillis = { testScheduler.currentTime }
        )
    }

    private fun interaction(id: String, body: String = "{}") = """{"interactionId":"$id","items":$body}"""

    private fun TestScope.receiving(sut: CompositePeerdroidConnector): List<DataChannelWrapperEvent> {
        val received = mutableListOf<DataChannelWrapperEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.dataChannelMessagesFromRemoteClients.toList(received)
        }
        runCurrent()
        return received
    }

    // ---- which transports connect

    @Test
    fun `by default links are connected over WebRTC only`() = runTest {
        val sut = composite(RadixConnectTransport.DEFAULT)

        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordA).isSuccess)

        assertEquals(listOf(idA), webRtc.connectedIds)
        assertTrue(iroh.connectedIds.isEmpty())
    }

    @Test
    fun `in automatic mode the links known to support iroh are connected over iroh too`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))

        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)

        assertEquals(listOf(idA, idB), webRtc.connectedIds)
        assertEquals(listOf(idA), iroh.connectedIds)
    }

    @Test
    fun `in WebRTC mode only WebRTC is used`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC, irohCapable = setOf(idA))

        sut.connectToConnectorExtension(transportProfile, passwordA)
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.isSuccess)
        assertEquals(listOf(idA), webRtc.connectedIds)
        assertEquals(listOf(idA to "message"), webRtc.sentMessages)
        assertTrue(iroh.connectedIds.isEmpty())
        assertTrue(iroh.sentMessages.isEmpty())
    }

    @Test
    fun `in iroh mode only iroh is used`() = runTest {
        val sut = composite(RadixConnectTransport.IROH)

        sut.connectToConnectorExtension(transportProfile, passwordA)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertEquals(listOf(idA), iroh.connectedIds)
        assertEquals(listOf(idA to "message"), iroh.sentMessages)
        assertTrue(webRtc.connectedIds.isEmpty())
        assertTrue(webRtc.sentMessages.isEmpty())
    }

    @Test
    fun `connecting succeeds if any transport connects`() = runTest {
        webRtc.connectResult = Result.failure(IllegalStateException("signaling server unreachable"))
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))

        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordA).isSuccess)
    }

    @Test
    fun `connecting fails when every transport fails`() = runTest {
        webRtc.connectResult = Result.failure(IllegalStateException("webrtc"))
        iroh.connectResult = Result.failure(IllegalStateException("iroh"))
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))

        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordA).isFailure)
    }

    // ---- learning and forgetting iroh support

    @Test
    fun `a link learned to support iroh is connected over iroh as well`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        settings.rememberIrohSupport(ConnectionIdHolder(passwordA))
        runCurrent()

        assertEquals(listOf(idA), iroh.connectedIds)
        assertEquals(listOf(idA), webRtc.connectedIds)
    }

    @Test
    fun `a link whose iroh session connects is remembered as supporting iroh`() = runTest {
        composite(RadixConnectTransport.IROH)

        iroh.setStatus(idA, PeerConnectionStatus.CLOSED)
        runCurrent()
        assertTrue(settings.remembered.isEmpty())

        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        runCurrent()
        assertEquals(listOf(idA), settings.remembered)
    }

    @Test
    fun `deleting a link removes it from every transport and forgets it`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.deleteConnector(ConnectionIdHolder(passwordA))
        runCurrent()

        assertEquals(listOf(idA), webRtc.deletedIds)
        assertEquals(listOf(idA), iroh.deletedIds)
        assertEquals(listOf(idA), settings.forgotten)
        assertTrue(webRtc.connectedIds.isEmpty())
        assertTrue(iroh.connectedIds.isEmpty())
    }

    // ---- status

    @Test
    fun `statuses of the transports are merged per connector`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA, idB))

        webRtc.statuses.value = mapOf(idA to PeerConnectionStatus.CLOSED, idB to PeerConnectionStatus.CONNECTING)
        iroh.statuses.value = mapOf(idA to PeerConnectionStatus.OPEN, idB to PeerConnectionStatus.CLOSED)

        assertEquals(
            mapOf(idA to PeerConnectionStatus.OPEN, idB to PeerConnectionStatus.CONNECTING),
            sut.peerConnectionStatus.first()
        )
    }

    @Test
    fun `merging statuses prefers open, then connecting, then closed`() {
        val merged = CompositePeerdroidConnector.mergeStatuses(
            listOf(
                mapOf(
                    "open-closed" to PeerConnectionStatus.OPEN,
                    "connecting-closed" to PeerConnectionStatus.CONNECTING,
                    "closed-closed" to PeerConnectionStatus.CLOSED,
                    "only-first" to PeerConnectionStatus.CONNECTING
                ),
                mapOf(
                    "open-closed" to PeerConnectionStatus.CLOSED,
                    "connecting-closed" to PeerConnectionStatus.CLOSED,
                    "closed-closed" to PeerConnectionStatus.CLOSED,
                    "only-second" to PeerConnectionStatus.OPEN
                )
            )
        )

        assertEquals(
            mapOf(
                "open-closed" to PeerConnectionStatus.OPEN,
                "connecting-closed" to PeerConnectionStatus.CONNECTING,
                "closed-closed" to PeerConnectionStatus.CLOSED,
                "only-first" to PeerConnectionStatus.CONNECTING,
                "only-second" to PeerConnectionStatus.OPEN
            ),
            merged
        )
    }

    @Test
    fun `a transport a link does not use does not contribute to its status`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        iroh.statuses.value = mapOf(idA to PeerConnectionStatus.OPEN)
        iroh.connected.value = true

        assertEquals(emptyMap<String, PeerConnectionStatus>(), sut.peerConnectionStatus.first())
        assertFalse(sut.anyChannelConnected.first())
    }

    @Test
    fun `any channel is connected when one of the used transports is`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        assertFalse(sut.anyChannelConnected.first())

        iroh.connected.value = true
        assertTrue(sut.anyChannelConnected.first())

        iroh.connected.value = false
        webRtc.connected.value = true
        assertTrue(sut.anyChannelConnected.first())
    }

    @Test
    fun `incoming messages of all transports are merged`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idB))
        val received = mutableListOf<DataChannelWrapperEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.dataChannelMessagesFromRemoteClients.toList(received)
        }
        runCurrent()

        val fromWebRtc = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), "webrtc")
        val fromIroh = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idB), "iroh")
        webRtc.messages.emit(fromWebRtc)
        iroh.messages.emit(fromIroh)
        runCurrent()

        assertEquals(listOf(fromWebRtc, fromIroh), received)
    }

    // ---- sending

    @Test
    fun `a message goes over iroh when the iroh session of a link supporting it is open`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)

        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.isSuccess)
        assertEquals(listOf(idA to "message"), iroh.sentMessages)
        assertTrue(webRtc.sentMessages.isEmpty())
    }

    @Test
    fun `a message goes over WebRTC when the iroh session is not open`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.setStatus(idA, PeerConnectionStatus.CLOSED)

        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.isSuccess)
        assertEquals(listOf(idA to "message"), webRtc.sentMessages)
        assertTrue(iroh.sentMessages.isEmpty())
    }

    @Test
    fun `a message to a link not known to support iroh never goes over iroh`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC)
        // Even when WebRTC did not send it and iroh would.
        webRtc.sendResult = { Result.failure(MessageNotSentException("not connected")) }
        iroh.sendResult = { Result.success(Unit) }

        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.exceptionOrNull() is MessageNotSentException)
        assertTrue(iroh.sentMessages.isEmpty())
    }

    @Test
    fun `a message not sent by the preferred transport falls back to the other one`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        // The iroh connection dropped right before sending: nothing was sent.
        iroh.sendResult = { Result.failure(MessageNotSentException("not connected")) }
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)

        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.isSuccess)
        assertEquals(listOf(idA to "message"), webRtc.sentMessages)
    }

    @Test
    fun `a message that may have been delivered is never sent again over the other transport`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        var irohAttempts = 0
        iroh.sendResult = {
            irohAttempts++
            Result.failure(IllegalStateException("the peer did not confirm the message in time"))
        }

        val result = sut.sendDataChannelMessageToRemoteClient(idA, "message")

        assertTrue(result.isFailure)
        assertEquals(1, irohAttempts)
        assertTrue(webRtc.sentMessages.isEmpty())
    }

    @Test
    fun `a message is sent once to every connected connector`() = runTest {
        val passwordC = password(seed = 3)
        val idC = ConnectionIdHolder(passwordC).id
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA, idB, idC))
        webRtc.statuses.value = mapOf(
            idA to PeerConnectionStatus.OPEN,
            idB to PeerConnectionStatus.OPEN,
            idC to PeerConnectionStatus.CLOSED
        )
        iroh.statuses.value = mapOf(
            idA to PeerConnectionStatus.OPEN,
            idB to PeerConnectionStatus.CLOSED,
            idC to PeerConnectionStatus.OPEN
        )

        val result = sut.sendDataChannelMessageToAllRemoteClients("message")

        assertTrue(result.isSuccess)
        assertEquals(setOf(idA to "message", idC to "message"), iroh.sentMessages.toSet())
        assertEquals(listOf(idB to "message"), webRtc.sentMessages)
        assertEquals(3, iroh.sentMessages.size + webRtc.sentMessages.size)
    }

    @Test
    fun `sending to all connectors fails when none is connected`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC)

        val result = sut.sendDataChannelMessageToAllRemoteClients("message")

        assertTrue(result.exceptionOrNull() is MessageNotSentException)
    }

    // ---- diagnostics

    @Test
    fun `diagnostics list every link with the transports it uses`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        webRtc.setStatus(idA, PeerConnectionStatus.CLOSED)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        webRtc.setStatus(idB, PeerConnectionStatus.OPEN)
        webRtc.diagnosticsOf = { id ->
            TransportDiagnostics.WebRtc(status = webRtc.statuses.value[id.id], lastError = null, signalingServer = "wss://ss")
        }
        iroh.diagnosticsOf = { id ->
            TransportDiagnostics.Iroh(status = iroh.statuses.value[id.id], lastError = "dial: timed out", session = null)
        }

        val diagnostics = sut.linkDiagnostics().associateBy { it.connectionId }

        val linkA = diagnostics.getValue(idA)
        assertEquals(PeerConnectionStatus.OPEN, linkA.status)
        // iroh first (preferred), then WebRTC
        assertEquals(
            listOf(
                TransportDiagnostics.Iroh(status = PeerConnectionStatus.OPEN, lastError = "dial: timed out", session = null),
                TransportDiagnostics.WebRtc(status = PeerConnectionStatus.CLOSED, lastError = null, signalingServer = "wss://ss")
            ),
            linkA.transports
        )
        // B is not known to support iroh: WebRTC only
        assertEquals(
            listOf(TransportDiagnostics.WebRtc(status = PeerConnectionStatus.OPEN, lastError = null, signalingServer = "wss://ss")),
            diagnostics.getValue(idB).transports
        )
    }

    @Test
    fun `deleted links are not in the diagnostics`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.deleteConnector(ConnectionIdHolder(passwordA))

        assertTrue(sut.linkDiagnostics().isEmpty())
    }

    // ---- policy changes

    @Test
    fun `switching from WebRTC to iroh removes the links from WebRTC and connects them over iroh`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        settings.transportState.value = RadixConnectTransport.IROH
        runCurrent()

        // Removed link by link and awaited, rather than with an asynchronous terminate.
        assertEquals(listOf(idA), webRtc.deletedIds)
        assertTrue(webRtc.connectedIds.isEmpty())
        assertEquals(listOf(idA), iroh.connectedIds)

        settings.transportState.value = RadixConnectTransport.WEBRTC
        runCurrent()

        assertEquals(listOf(idA), iroh.deletedIds)
        assertTrue(iroh.connectedIds.isEmpty())
        assertEquals(listOf(idA), webRtc.connectedIds)
    }

    @Test
    fun `switching to automatic keeps WebRTC and adds iroh for the links supporting it`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)

        settings.transportState.value = RadixConnectTransport.AUTOMATIC
        runCurrent()

        assertTrue(webRtc.deletedIds.isEmpty())
        assertEquals(listOf(idA, idB), webRtc.connectedIds)
        assertEquals(listOf(idA), iroh.connectedIds)
    }

    @Test
    fun `quick switches back and forth leave the final transport connected`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        settings.transportState.value = RadixConnectTransport.IROH
        settings.transportState.value = RadixConnectTransport.WEBRTC
        runCurrent()

        assertEquals(listOf(idA), webRtc.connectedIds)
        assertTrue(iroh.connectedIds.isEmpty())
    }

    @Test
    fun `deleted links are not connected when the policy changes`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)

        sut.deleteConnector(ConnectionIdHolder(passwordA))
        settings.transportState.value = RadixConnectTransport.IROH
        runCurrent()

        // A by the delete (on every transport), then B because WebRTC was switched off.
        assertEquals(listOf(idA, idB), webRtc.deletedIds)
        assertEquals(listOf(idA), iroh.deletedIds)
        assertEquals(listOf(idB), iroh.connectedIds)
    }

    @Test
    fun `terminated links are not connected when the policy changes`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.terminateConnectionToConnectorExtension()
        settings.transportState.value = RadixConnectTransport.IROH
        runCurrent()

        assertTrue(iroh.connectedIds.isEmpty())
    }

    @Test
    fun `a link deleted while it is being connected ends up disconnected`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        val signalingServer = CompletableDeferred<Unit>()
        webRtc.connectGate = signalingServer

        val connecting = async { sut.connectToConnectorExtension(transportProfile, passwordA) }
        runCurrent()
        // Not held up by the connection in progress.
        sut.deleteConnector(ConnectionIdHolder(passwordA))
        signalingServer.complete(Unit)
        connecting.await()

        assertTrue(webRtc.connectedIds.isEmpty())
    }

    @Test
    fun `a policy change is not held up by a connection in progress`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        val signalingServer = CompletableDeferred<Unit>()
        webRtc.connectGate = signalingServer

        val connecting = async { sut.connectToConnectorExtension(transportProfile, passwordA) }
        runCurrent()
        settings.transportState.value = RadixConnectTransport.IROH
        runCurrent()

        assertEquals(listOf(idA), iroh.connectedIds)

        signalingServer.complete(Unit)
        connecting.await()

        // The WebRTC connection that completed after the switch is removed again.
        assertTrue(webRtc.connectedIds.isEmpty())
    }

    @Test
    fun `a link requested before the policy is known follows the stored settings`() = runTest {
        settings.transportState.value = RadixConnectTransport.IROH
        // An application scope that has not started collecting the settings yet.
        val notStartedScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val sut = CompositePeerdroidConnector(
            connectors = mapOf(PeerdroidTransport.WEBRTC to webRtc, PeerdroidTransport.IROH to iroh),
            settings = settings,
            applicationScope = notStartedScope,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        sut.connectToConnectorExtension(transportProfile, passwordA)

        assertEquals(listOf(idA), iroh.connectedIds)
        assertTrue(webRtc.connectedIds.isEmpty())
        notStartedScope.cancel()
    }

    // ---- diagnostics: logs and recent events

    @Test
    fun `the transport that carried a message is logged with the interaction id, never the contents`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        val message = interaction(id = "4c1d2b6e-0f6e-4b0a-9a7e-2f1d3c4b5a69", body = """{"persona":"Alice"}""")

        sut.sendDataChannelMessageToRemoteClient(idA, message)

        val sent = logs.linesWith("sent interaction=4c1d2b6e-0f6e-4b0a-9a7e-2f1d3c4b5a69").single()
        assertTrue(sent.message, sent.message.contains("link=${idA.take(8)} sent interaction=4c1d2b6e-0f6e-4b0a-9a7e-2f1d3c4b5a69 (${message.length} chars) over iroh"))
        assertTrue(sent.message, sent.message.endsWith("(connected: [iroh, webrtc], order: [iroh, webrtc])"))
        assertEquals(RadixConnectLog.TAG, sent.tag)
        assertTrue(logs.linesWith("Alice").isEmpty())
    }

    @Test
    fun `a fallback is logged with why the preferred transport did not send the message`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.sendResult = { Result.failure(MessageNotSentException("not connected")) }
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)

        sut.sendDataChannelMessageToRemoteClient(idA, interaction(id = "abc"))

        assertEquals(
            listOf("W", "I"),
            (logs.linesWith("interaction=abc")).map { it.toString().substringBefore("/") }
        )
        assertTrue(logs.linesWith("iroh did not send it (not sent, MessageNotSentException), falling back to webrtc").isNotEmpty())
        assertTrue(logs.linesWith("sent interaction=abc (${interaction(id = "abc").length} chars) over webrtc").isNotEmpty())
        assertEquals(
            "message fell back from iroh to webrtc",
            sut.linkDiagnostics().single { it.connectionId == idA }.recentEvents.last().description
        )
    }

    @Test
    fun `a message that may have been delivered is logged as an error, without a fallback`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.sendResult = { Result.failure(IllegalStateException("the peer did not confirm the message in time")) }

        sut.sendDataChannelMessageToRemoteClient(idA, interaction(id = "abc"))

        val error = logs.linesWith("could not send interaction=abc").single()
        assertEquals("E", error.toString().substringBefore("/"))
        assertTrue(error.message, error.message.contains("iroh failed (maybe delivered, IllegalStateException)"))
        assertTrue(logs.linesWith("falling back").isEmpty())
    }

    @Test
    fun `the same interaction arriving over both transports is handed over once`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        val received = receiving(sut)
        val message = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), interaction(id = "abc"))

        iroh.messages.emit(message)
        advanceTimeBy(1_500.milliseconds)
        webRtc.messages.emit(message)
        runCurrent()

        assertEquals(listOf(message), received)
        assertTrue(
            logs.linesWith("dropped a duplicate of interaction=abc (${interaction(id = "abc").length} chars) over webrtc: first received over iroh 1500 ms earlier")
                .isNotEmpty()
        )
    }

    @Test
    fun `the same interaction is handed over again once the duplicate window is over`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        val received = receiving(sut)
        val message = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), interaction(id = "abc"))

        iroh.messages.emit(message)
        advanceTimeBy(IncomingMessageDeduplicator.DEFAULT_WINDOW_MS.milliseconds)
        iroh.messages.emit(message)
        runCurrent()

        assertEquals(listOf(message, message), received)
    }

    @Test
    fun `on a link over WebRTC only, the same interaction arriving again is handed over again`() = runTest {
        val sut = composite(RadixConnectTransport.WEBRTC)
        val received = receiving(sut)
        val message = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), interaction(id = "abc"))

        webRtc.messages.emit(message)
        advanceTimeBy(1_500.milliseconds)
        webRtc.messages.emit(message)
        runCurrent()

        assertEquals(listOf(message, message), received)
    }

    @Test
    fun `different interactions and messages without an interaction id are never dropped`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        val received = receiving(sut)
        val first = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), interaction(id = "abc"))
        val second = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), interaction(id = "def"))
        val noId = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idA), "{}")
        // The same interaction on another link is another message.
        val otherLink = DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(idB), interaction(id = "abc"))

        listOf(first, second, noId, noId, otherLink).forEach { iroh.messages.emit(it) }
        runCurrent()

        assertEquals(listOf(first, second, noId, noId, otherLink), received)
        assertEquals(1, logs.linesWith("received interaction=abc (${interaction(id = "abc").length} chars) over iroh").count { "link=${idA.take(8)}" in it.message })
    }

    @Test
    fun `every change of the merged status of a link is logged with the status of each transport`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)

        listOf(
            webRtc to PeerConnectionStatus.CONNECTING,
            webRtc to PeerConnectionStatus.OPEN,
            iroh to PeerConnectionStatus.OPEN,
            webRtc to PeerConnectionStatus.CLOSED,
            iroh to PeerConnectionStatus.CLOSED
        ).forEach { (transport, status) ->
            transport.setStatus(idA, status)
            runCurrent()
        }
        advanceTimeBy(2_000.milliseconds)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        runCurrent()

        assertEquals(
            listOf(
                "status: not running → connecting (iroh=not running, webrtc=connecting)",
                "status: connecting → open (iroh=not running, webrtc=open)",
                "status: open → closed (iroh=closed, webrtc=closed)",
                "status: closed → open (iroh=open, webrtc=closed, connected again after 2000 ms)"
            ),
            logs.linesWith("status:").map { it.message.substringAfter("link=${idA.take(8)} ") }
        )
    }

    @Test
    fun `the app foreground is passed on to every transport`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.onAppForeground()

        assertEquals(1, iroh.appForegroundCount)
        assertEquals(1, webRtc.appForegroundCount)
    }

    @Test
    fun `whether the app is in front is passed on to every transport`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.onAppInFront(false)
        sut.onAppInFront(true)

        assertEquals(listOf(false, true), iroh.inFrontToldValues)
        assertEquals(listOf(false, true), webRtc.inFrontToldValues)
    }

    @Test
    fun `the app background is passed on to every transport, and WebRTC's connections are left alone`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        webRtc.setStatus(idA, PeerConnectionStatus.OPEN)

        sut.onAppBackground()
        runCurrent()

        assertEquals(1, iroh.appBackgroundCount)
        assertEquals(1, webRtc.appBackgroundCount)
        assertTrue(webRtc.deletedIds.isEmpty())
        assertEquals(0, webRtc.terminateCount)
        assertEquals(listOf(idA), webRtc.connectedIds)
        assertEquals(PeerConnectionStatus.OPEN, sut.peerConnectionStatus.first()[idA])
    }

    @Test
    fun `the diagnostics hold the recent events of the link and of its transports, oldest first`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        iroh.recentEventsOf = { listOf(LinkEvent(atMillis = 0, source = "iroh", description = "session started (connect)")) }

        val events = sut.linkDiagnostics().single().recentEvents

        assertEquals(LinkEvent(atMillis = 0, source = "iroh", description = "session started (connect)"), events.first())
        assertEquals("link", events.last().source)
        assertTrue(events.last().description, events.last().description.startsWith("connecting over [iroh, webrtc]"))
    }

    @Test
    fun `neither the diagnostics nor the logs ever hold the password or the full connection id`() = runTest {
        val sut = composite(RadixConnectTransport.AUTOMATIC, irohCapable = setOf(idA))
        webRtc.connectResult = Result.failure(IllegalStateException("signaling server unreachable"))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        iroh.setStatus(idA, PeerConnectionStatus.OPEN)
        iroh.sendResult = { Result.failure(MessageNotSentException("not connected")) }
        sut.sendDataChannelMessageToRemoteClient(idA, interaction(id = "abc"))
        iroh.setStatus(idA, PeerConnectionStatus.CLOSED)

        val diagnostics = sut.linkDiagnostics().single()
        val password = passwordA.value.hex

        assertFalse(diagnostics.toString().contains(password))
        assertFalse(diagnostics.recentEvents.toString().contains(idA))
        assertTrue(logs.lines.isNotEmpty())
        assertNull(logs.lines.firstOrNull { password in it.message || idA in it.message })
    }
}
