package rdx.works.peerdroid.data.iroh

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
import rdx.works.peerdroid.data.fakes.FakeIrohSessionFactory
import rdx.works.peerdroid.data.fakes.FakeNetworkChangeMonitor
import rdx.works.peerdroid.data.fakes.FakeTransportSettings
import rdx.works.peerdroid.data.fakes.RecordingTree
import rdx.works.peerdroid.data.fakes.password
import rdx.works.peerdroid.data.fakes.transportProfile
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.IrohPath
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionStatus
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class IrohPeerdroidConnectorTest {

    @get:Rule
    val logs = RecordingTree()

    private val factory = FakeIrohSessionFactory()
    private val settings = FakeTransportSettings()
    private val networkMonitor = FakeNetworkChangeMonitor()

    private val passwordA = password(seed = 1)
    private val passwordB = password(seed = 2)
    private val idA = ConnectionIdHolder(passwordA)
    private val idB = ConnectionIdHolder(passwordB)

    private val received = mutableListOf<DataChannelWrapperEvent>()

    private lateinit var collector: Job

    private fun TestScope.connector(
        relayUrl: String? = null,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
    ): IrohPeerdroidConnector {
        settings.relayUrlState.value = relayUrl
        return IrohPeerdroidConnector(
            sessionFactory = factory,
            settings = settings,
            networkChangeMonitor = networkMonitor,
            applicationScope = backgroundScope,
            ioDispatcher = ioDispatcher,
            monotonicMillis = { testScheduler.currentTime }
        )
    }

    // Listens for messages, like the app does while its main screen exists.
    private fun TestScope.collectingJob(connector: IrohPeerdroidConnector): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            connector.dataChannelMessagesFromRemoteClients.toList(received)
        }

    @Test
    fun `one wallet session is started per link`() = runTest {
        val sut = connector()

        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordA).isSuccess)
        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordA).isSuccess)
        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordB).isSuccess)

        assertEquals(listOf(idA, idB), factory.sessions.map { it.connectionId })
        assertNull(factory.sessions.first().relayUrl)
    }

    @Test
    fun `the configured relay is used`() = runTest {
        val sut = connector(relayUrl = " https://relay.example.com ")

        sut.connectToConnectorExtension(transportProfile, passwordA)

        assertEquals("https://relay.example.com", factory.sessions.single().relayUrl)
    }

    @Test
    fun `an unusable configured relay falls back to the public relays`() = runTest {
        val sut = connector(relayUrl = "ftp://relay.example.com")

        sut.connectToConnectorExtension(transportProfile, passwordA)

        assertNull(factory.sessions.single().relayUrl)
    }

    @Test
    fun `a relay that cannot be validated falls back to the public relays instead of failing`() = runTest {
        factory.validationError = IllegalStateException("the radix-connect-iroh native library is not available")
        val sut = connector(relayUrl = "https://relay.example.com")

        val result = sut.connectToConnectorExtension(transportProfile, passwordA)

        assertTrue(result.isSuccess)
        assertNull(factory.sessions.single().relayUrl)
    }

    @Test
    fun `a session that cannot start is reported as a failure`() = runTest {
        factory.startError = IllegalStateException("could not bind")
        val sut = connector()

        val result = sut.connectToConnectorExtension(transportProfile, passwordA)

        assertTrue(result.isFailure)
        assertEquals(emptyMap<String, PeerConnectionStatus>(), sut.peerConnectionStatus.first())
    }

    @Test
    fun `the status follows the state of the session`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()

        assertEquals(mapOf(idA.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())
        assertFalse(sut.anyChannelConnected.first())

        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        assertEquals(mapOf(idA.id to PeerConnectionStatus.OPEN), sut.peerConnectionStatus.first())
        assertTrue(sut.anyChannelConnected.first())

        // Dialing an extension that may not be running is not reported as connecting (see IrohPeerdroidConnector).
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTING))
        runCurrent()
        assertEquals(mapOf(idA.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())

        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.DISCONNECTED))
        runCurrent()
        assertEquals(mapOf(idA.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())
        assertFalse(sut.anyChannelConnected.first())
    }

    @Test
    fun `a session starts with what the previous session of its link remembered, and keeps what it remembers`() =
        runTest {
            settings.linkMemories[idA.id] = "kept before"
            val sut = connector()
            collector = collectingJob(sut)
            sut.connectToConnectorExtension(transportProfile, passwordA)
            val session = factory.sessions.single()
            assertEquals("kept before", session.startedWithMemory)

            session.memory = "connected just now"
            session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
            runCurrent()
            assertEquals("connected just now", settings.linkMemories[idA.id])

            // Stopping keeps the latest one too, for the next session.
            session.memory = "stopped just now"
            sut.onAppBackground()
            advanceTimeBy(60.seconds)
            runCurrent()
            assertTrue(session.isShutDown)
            assertEquals("stopped just now", settings.linkMemories[idA.id])
        }

    @Test
    fun `a memory read while the link is deleted is not written back`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        val gate = CompletableDeferred<Unit>()
        session.memoryGate = gate

        // Connected: the memory is being read, slowly, when the link is deleted and then forgotten, as the app does.
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        val deleting = async {
            sut.deleteConnector(idA)
            settings.forgetLink(idA)
        }
        runCurrent()
        gate.complete(Unit)
        deleting.await()
        runCurrent()

        assertNull(settings.linkMemories[idA.id])
    }

    @Test
    fun `nothing is kept of a deleted link`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.memory = "should not be kept"

        sut.deleteConnector(idA)

        assertNull(settings.linkMemories[idA.id])
    }

    @Test
    fun `incoming messages are forwarded with the connection id of their link`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)

        factory.sessions.single().emit(IrohEvent.Message("""{"discriminator":"accountList"}"""))
        runCurrent()

        assertEquals(
            listOf(DataChannelWrapperEvent.MessageFromRemoteConnectionId(idA, """{"discriminator":"accountList"}""")),
            received
        )
    }

    @Test
    fun `nothing is taken from the session while nobody collects the messages`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()

        session.emit(IrohEvent.Message("""{"discriminator":"dappInteraction"}"""))
        advanceTimeBy(5.seconds)

        // Not taken, hence not confirmed: the connector extension will send it again if needed.
        assertEquals(0, session.takenEvents)
        assertEquals(0, session.pendingReads)

        collector = collectingJob(sut)
        runCurrent()

        assertEquals(1, session.takenEvents)
        assertEquals(1, received.size)
    }

    @Test
    fun `nothing more is taken once the last collector goes away`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        runCurrent()
        assertEquals(1, session.pendingReads)

        // e.g. the main screen goes away and its listeners stop before the sessions do
        collector.cancel()
        runCurrent()
        session.emit(IrohEvent.Message("""{"discriminator":"dappInteraction"}"""))
        advanceTimeBy(5.seconds)

        assertEquals(0, session.pendingReads)
        assertEquals(0, session.takenEvents)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `the status stays fresh while nobody collects the messages`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()

        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        advanceTimeBy(2.seconds)

        assertEquals(mapOf(idA.id to PeerConnectionStatus.OPEN), sut.peerConnectionStatus.first())
        assertEquals(0, session.takenEvents)
    }

    @Test
    fun `sending to a link without a session reports that nothing was sent`() = runTest {
        val sut = connector()

        val result = sut.sendDataChannelMessageToRemoteClient(idA.id, "message")

        assertTrue(result.exceptionOrNull() is MessageNotSentException)
    }

    @Test
    fun `messages are sent through the session of the link`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        val result = sut.sendDataChannelMessageToRemoteClient(idA.id, "message")

        assertTrue(result.isSuccess)
        assertEquals(listOf("message"), session.sentMessages)
    }

    @Test
    fun `send failures are reported unchanged`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        val timeout = IllegalStateException("the peer did not confirm the message in time")
        session.sendError = timeout

        val result = sut.sendDataChannelMessageToRemoteClient(idA.id, "message")

        assertEquals(timeout, result.exceptionOrNull())
    }

    @Test
    fun `a message for all links is sent to the connected ones`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        val (sessionA, sessionB) = factory.sessions
        sessionA.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        val result = sut.sendDataChannelMessageToAllRemoteClients("message")

        assertTrue(result.isSuccess)
        assertEquals(listOf("message"), sessionA.sentMessages)
        assertTrue(sessionB.sentMessages.isEmpty())
    }

    @Test
    fun `deleting a link shuts its session down and removes its status`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)

        sut.deleteConnector(idA)

        assertTrue(factory.sessionsOf(passwordA).single().isShutDown)
        assertFalse(factory.sessionsOf(passwordB).single().isShutDown)
        assertEquals(mapOf(idB.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())
    }

    @Test
    fun `terminating shuts every session down`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)

        sut.terminateConnectionToConnectorExtension()
        runCurrent()

        assertTrue(factory.sessions.all { it.isShutDown })
        assertEquals(emptyMap<String, PeerConnectionStatus>(), sut.peerConnectionStatus.first())

        // and a link can be connected again afterwards
        sut.connectToConnectorExtension(transportProfile, passwordA)
        assertEquals(1, factory.activeSessions().size)
    }

    @Test
    fun `a link connected right after terminating keeps its session`() = runTest {
        val sut = connector(ioDispatcher = StandardTestDispatcher(testScheduler))
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val sessionBeforeTerminate = factory.sessions.single()

        // The terminate runs asynchronously: the connect below gets the link's lock first.
        sut.terminateConnectionToConnectorExtension()
        val result = sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()

        assertTrue(result.isSuccess)
        assertTrue(sessionBeforeTerminate.isShutDown)
        val session = factory.activeSessions().single()
        assertEquals(idA, session.connectionId)
        assertEquals(mapOf(idA.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())
    }

    @Test
    fun `a slow close holds up only its own link, and its restart waits for it`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val sessionA = factory.sessions.single()
        val closeA = CompletableDeferred<Unit>()
        sessionA.shutdownGate = closeA

        val deleteA = async { sut.deleteConnector(idA) }
        runCurrent()
        assertFalse(deleteA.isCompleted)

        // another link is not held up
        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordB).isSuccess)

        // the same link (same endpoint key) starts only once the old session is closed
        val reconnectA = async { sut.connectToConnectorExtension(transportProfile, passwordA) }
        runCurrent()
        assertEquals(1, factory.sessionsOf(passwordA).size)

        closeA.complete(Unit)
        runCurrent()
        deleteA.await()
        assertTrue(reconnectA.await().isSuccess)
        assertEquals(2, factory.sessionsOf(passwordA).size)
    }

    @Test
    fun `changing the relay restarts the sessions with the new relay`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val oldSession = factory.sessions.single()

        settings.relayUrlState.value = "https://relay.example.com"
        runCurrent()

        assertTrue(oldSession.isShutDown)
        val newSession = factory.activeSessions().single()
        assertEquals(idA, newSession.connectionId)
        assertEquals("https://relay.example.com", newSession.relayUrl)
    }

    @Test
    fun `network changes are reported to every session once per burst`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        runCurrent()

        repeat(3) { networkMonitor.changes.emit(Unit) }
        advanceTimeBy(1_000)

        assertEquals(listOf(1, 1), factory.sessions.map { it.networkChangedCount })
    }

    @Test
    fun `the network is observed only while a session exists`() = runTest {
        val sut = connector()
        assertEquals(0, networkMonitor.changes.subscriptionCount.value)

        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        assertEquals(1, networkMonitor.changes.subscriptionCount.value)

        sut.deleteConnector(idA)
        runCurrent()
        assertEquals(0, networkMonitor.changes.subscriptionCount.value)
    }

    @Test
    fun `a paused session is stopped during the block and started again afterwards`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val sessionBeforePause = factory.sessions.single()

        val result = sut.withSessionPaused(idA) {
            assertTrue(sessionBeforePause.isShutDown)
            // e.g. the link is saved while linking: the regular session must wait for the linking to end
            sut.connectToConnectorExtension(transportProfile, passwordA)
            assertTrue(factory.activeSessions().isEmpty())
            "linked"
        }

        assertEquals("linked", result)
        assertEquals(2, factory.sessions.size)
        assertEquals(idA, factory.activeSessions().single().connectionId)
    }

    @Test
    fun `a link connected while paused is started once the pause ends`() = runTest {
        val sut = connector()

        sut.withSessionPaused(idA) {
            sut.connectToConnectorExtension(transportProfile, passwordA)
            assertTrue(factory.sessions.isEmpty())
        }

        assertEquals(idA, factory.activeSessions().single().connectionId)
    }

    @Test
    fun `a link deleted while paused is not started again`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.withSessionPaused(idA) {
            sut.deleteConnector(idA)
        }

        assertTrue(factory.activeSessions().isEmpty())
    }

    @Test
    fun `a link terminated while paused is not started again`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.withSessionPaused(idA) {
            sut.terminateConnectionToConnectorExtension()
        }
        runCurrent()

        assertTrue(factory.activeSessions().isEmpty())
    }

    @Test
    fun `the diagnostics of a link report its session`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        val diagnostics = sut.diagnostics(idA)

        assertEquals(PeerConnectionStatus.OPEN, diagnostics.status)
        val session = requireNotNull(diagnostics.session)
        assertTrue(session.usesPublicRelays)
        assertEquals(IrohPath.DIRECT, session.path)
        assertEquals(42L, session.rttMs)
        assertTrue(session.isRelayConnected)
    }

    @Test
    fun `a link that could not start reports why`() = runTest {
        factory.startError = IllegalStateException("invalid relay URL")
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)

        val diagnostics = sut.diagnostics(idA)

        assertNull(diagnostics.status)
        assertNull(diagnostics.session)
        assertEquals("start: IllegalStateException invalid relay URL", diagnostics.lastError)
    }

    @Test
    fun `a failed send is reported without the message contents`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        session.sendError = IllegalStateException("the peer did not confirm the message in time")

        sut.sendDataChannelMessageToRemoteClient(idA.id, """{"secret":"value"}""")

        val lastError = requireNotNull(sut.diagnostics(idA).lastError)
        assertTrue(lastError.startsWith("send: "))
        assertFalse(lastError.contains("secret"))
    }

    @Test
    fun `a message taken by the linking session is handed over once somebody collects the messages`() = runTest {
        val sut = connector()

        sut.deliverReceivedMessage(idA, "{\"interactionId\":\"1\"}")
        runCurrent()
        assertTrue(received.isEmpty())

        collector = collectingJob(sut)
        runCurrent()

        assertEquals(listOf(DataChannelWrapperEvent.MessageFromRemoteConnectionId(idA, "{\"interactionId\":\"1\"}")), received)
    }

    @Test
    fun `iroh messages never expose their contents through toString`() {
        assertFalse(IrohEvent.Message("""{"secret":"value"}""").toString().contains("secret"))
    }

    // ---- diagnostics: logs and recent events

    @Test
    fun `every connect and disconnect is logged with the library's snapshot and kept as an event`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()

        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.DISCONNECTED))
        runCurrent()

        assertEquals(
            listOf(
                "link=${idA.id.take(8)} iroh started → connected: wallet 1111111111 → 2222222222 state=connected",
                "link=${idA.id.take(8)} iroh connected → disconnected: wallet 1111111111 → 2222222222 state=disconnected"
            ),
            logs.linesWith(" iroh ").filter { "→" in it.message && "state=" in it.message }.map { it.message.substringAfter("🧊 ") }
        )
        assertEquals(
            listOf("session started (connect)", "started → connected", "connected → disconnected"),
            sut.recentEvents(idA).map { it.description }
        )
    }

    @Test
    fun `a stopped session is logged with why and with its last snapshot`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)

        sut.deleteConnector(idA)

        val line = logs.linesWith("iroh session stopping").single()
        assertTrue(
            line.message,
            line.message.endsWith("(the link is deleted or no longer uses iroh): wallet 1111111111 → 2222222222 state=connecting")
        )
    }

    @Test
    fun `a message the connector extension did not confirm in time is reported with the limit`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        session.sendError = MessageNotConfirmedException(limitMs = 10_000)

        val result = sut.sendDataChannelMessageToRemoteClient(idA.id, "{}")

        assertTrue(result.exceptionOrNull() is MessageNotConfirmedException)
        assertEquals("send: not confirmed within the 10000 ms limit (maybe delivered)", sut.diagnostics(idA).lastError)
        assertEquals("a message failed: not confirmed within the 10000 ms limit (maybe delivered)", sut.recentEvents(idA).last().description)
    }

    @Test
    fun `forwarded network changes are logged once per burst and link`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        runCurrent()

        repeat(3) { networkMonitor.changes.emit(Unit) }
        advanceTimeBy(1_000)

        assertEquals(1, logs.linesWith("link=${idA.id.take(8)} checking the iroh connection: network change").size)
        assertEquals(1, logs.linesWith("link=${idB.id.take(8)} checking the iroh connection: network change").size)
        assertEquals("connection check (network change)", sut.recentEvents(idB).last().description)
    }

    // Link B's connector has not been connected in the last day.
    private fun bIsDormant() {
        settings.linkMemories[idB.id] = DORMANT_MEMORY
        factory.staleMemories += DORMANT_MEMORY
    }

    @Test
    fun `starting from scratch, a dormant link starts once the live ones are connected`() = runTest {
        bIsDormant()
        val sut = connector()
        collector = collectingJob(sut)

        sut.connectToConnectorExtension(transportProfile, passwordA)
        assertTrue(sut.connectToConnectorExtension(transportProfile, passwordB).isSuccess)
        runCurrent()
        assertEquals(listOf(idA), factory.sessions.map { it.connectionId })

        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        assertEquals(listOf(idA, idB), factory.sessions.map { it.connectionId })
    }

    @Test
    fun `starting from scratch, a dormant link waits for the live ones a few seconds at most`() = runTest {
        bIsDormant()
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        runCurrent()

        advanceTimeBy(2_900)
        assertEquals(listOf(idA), factory.sessions.map { it.connectionId })
        advanceTimeBy(200)

        assertEquals(listOf(idA, idB), factory.sessions.map { it.connectionId })
    }

    @Test
    fun `once started, a dormant link connects at once`() = runTest {
        bIsDormant()
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        // A link connected: the cold start is over.
        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        advanceTimeBy(3_100)

        sut.connectToConnectorExtension(transportProfile, passwordB)

        assertEquals(listOf(idA, idB), factory.sessions.map { it.connectionId })
    }

    @Test
    fun `a dormant link deleted while it waits never starts`() = runTest {
        bIsDormant()
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        runCurrent()

        sut.deleteConnector(idB)
        advanceTimeBy(3_100)

        assertEquals(listOf(idA), factory.sessions.map { it.connectionId })
    }

    @Test
    fun `coming back to the foreground makes every session check its connection, even a connected one`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        factory.sessions.first().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        assertEquals(PeerConnectionStatus.OPEN, sut.peerConnectionStatus.first()[idA.id])

        sut.onAppForeground()
        advanceTimeBy(1_000)

        assertEquals(listOf(1, 1), factory.sessions.map { it.networkChangedCount })
        assertEquals(1, logs.linesWith("link=${idA.id.take(8)} checking the iroh connection: app foreground").size)
        assertTrue(sut.recentEvents(idA).any { it.description == "connection check (app foreground)" })
    }

    @Test
    fun `a network change and the foreground together check each session once`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()

        networkMonitor.changes.emit(Unit)
        sut.onAppForeground()
        advanceTimeBy(1_000)

        assertEquals(1, factory.sessions.single().networkChangedCount)
        assertEquals(1, logs.linesWith("checking the iroh connection: app foreground, network change").size)
    }

    @Test
    fun `the foreground does nothing without a running session`() = runTest {
        val sut = connector()

        sut.onAppForeground()
        advanceTimeBy(1_000)

        assertTrue(factory.sessions.isEmpty())
        assertTrue(logs.linesWith("checking the iroh connection").isEmpty())
    }

    @Test
    fun `the same link linked twice at once is reported`() = runTest {
        val sut = connector()

        sut.withSessionPaused(idA) { sut.withSessionPaused(idA) { Unit } }

        assertEquals(1, logs.linesWith("link=${idA.id.take(8)} is being linked 2 times at once").size)
    }

    // ---- the app in the background

    private val grace = IrohBackgroundStop.BACKGROUND_GRACE.inWholeMilliseconds

    @Test
    fun `the sessions stop once the app has been in the background for the grace period`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        sut.connectToConnectorExtension(transportProfile, passwordB)
        factory.sessions.forEach { it.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED)) }
        runCurrent()

        sut.onAppBackground()
        advanceTimeBy(grace - 1)
        assertEquals(2, factory.activeSessions().size)

        advanceTimeBy(2)
        assertTrue(factory.activeSessions().isEmpty())
        assertEquals(mapOf(idA.id to PeerConnectionStatus.CLOSED, idB.id to PeerConnectionStatus.CLOSED), sut.peerConnectionStatus.first())
        assertEquals(2, logs.linesWith("iroh session stopping (app in background)").size)
        // Nothing is watched anymore: no network change reaches a session.
        assertEquals(0, networkMonitor.changes.subscriptionCount.value)
    }

    @Test
    fun `the connectors are told whether the app is in the background`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        assertFalse(factory.sessions.single().toldInBackground)

        sut.onAppBackground()
        assertTrue(factory.sessions.single().toldInBackground)
        // A session started meanwhile says so too.
        sut.connectToConnectorExtension(transportProfile, passwordB)
        runCurrent()
        assertTrue(factory.sessions.all { it.toldInBackground })

        sut.onAppInFront(true)
        runCurrent()
        assertTrue(factory.sessions.none { it.toldInBackground })
    }

    @Test
    fun `the connectors are told as soon as the app leaves the front, before the background, and nothing stops`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        // On the recent apps screen: paused, not stopped.
        sut.onAppInFront(false)
        assertTrue(factory.sessions.single().toldInBackground)
        advanceTimeBy(grace * 2)
        assertEquals(1, factory.activeSessions().size)

        sut.onAppInFront(true)
        assertFalse(factory.sessions.single().toldInBackground)
    }

    @Test
    fun `coming back within the grace period stops nothing`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()

        sut.onAppBackground()
        advanceTimeBy(grace / 2)
        sut.onAppForeground()
        advanceTimeBy(grace * 2)

        assertEquals(1, factory.sessions.size)
        assertFalse(factory.sessions.single().isShutDown)
        assertTrue(logs.linesWith("iroh session stopping").isEmpty())
    }

    @Test
    fun `coming back after the stop restarts the sessions and logs how long each link took to connect`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        sut.onAppBackground()
        advanceTimeBy(grace + 1)
        assertTrue(factory.activeSessions().isEmpty())

        sut.onAppForeground()
        runCurrent()
        val restarted = factory.activeSessions().single()
        assertEquals(idA, restarted.connectionId)
        advanceTimeBy(350)
        restarted.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        assertEquals(PeerConnectionStatus.OPEN, sut.peerConnectionStatus.first()[idA.id])
        assertEquals(1, logs.linesWith("link=${idA.id.take(8)} iroh session started (app foreground)").size)
        assertEquals(1, logs.linesWith("link=${idA.id.take(8)} iroh connected 350 ms after the app came to the foreground").size)
    }

    @Test
    fun `a session kept through a short background reports that it is still connected after the check`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        sut.onAppBackground()
        advanceTimeBy(1_000)
        sut.onAppForeground()
        advanceTimeBy(1_000)

        assertEquals(1, factory.sessions.single().networkChangedCount)
        assertEquals(1, logs.linesWith("iroh still connected 500 ms after the app came to the foreground").size)
    }

    @Test
    fun `a link connected while the app is in the background starts when it comes back`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        sut.onAppBackground()
        advanceTimeBy(grace + 1)

        sut.connectToConnectorExtension(transportProfile, passwordB)
        assertTrue(factory.activeSessions().isEmpty())

        sut.onAppForeground()
        runCurrent()
        assertEquals(setOf(idA, idB), factory.activeSessions().map { it.connectionId }.toSet())
    }

    @Test
    fun `a link deleted while the app is in the background does not start again`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        sut.onAppBackground()
        advanceTimeBy(grace + 1)

        sut.deleteConnector(idA)
        sut.onAppForeground()
        runCurrent()

        assertTrue(factory.activeSessions().isEmpty())
    }

    @Test
    fun `a message being sent delays the stop until it is confirmed`() = runTest {
        val sut = connector()
        collector = collectingJob(sut)
        sut.connectToConnectorExtension(transportProfile, passwordA)
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()
        val confirmed = CompletableDeferred<Unit>()
        session.sendGate = confirmed
        val sending = async { sut.sendDataChannelMessageToRemoteClient(idA.id, "{}") }

        sut.onAppBackground()
        advanceTimeBy(grace + 3_000)
        assertFalse(session.isShutDown)

        confirmed.complete(Unit)
        runCurrent()

        assertTrue(sending.await().isSuccess)
        assertTrue(session.isShutDown)
        assertEquals(grace + 3_000, testScheduler.currentTime)
    }

    @Test
    fun `a message waiting for the app delays the stop at most by the limit, and is still handed over`() = runTest {
        val sut = connector()
        sut.connectToConnectorExtension(transportProfile, passwordA)
        runCurrent()
        // Confirmed to the connector extension, while nobody listens.
        sut.deliverReceivedMessage(idA, "{\"interactionId\":\"1\"}")

        sut.onAppBackground()
        advanceTimeBy(grace + IrohBackgroundStop.IN_FLIGHT_LIMIT.inWholeMilliseconds - 1)
        assertFalse(factory.sessions.single().isShutDown)
        advanceTimeBy(2)
        assertTrue(factory.sessions.single().isShutDown)

        collector = collectingJob(sut)
        runCurrent()
        assertEquals(listOf(DataChannelWrapperEvent.MessageFromRemoteConnectionId(idA, "{\"interactionId\":\"1\"}")), received)
    }

    private companion object {
        // What the fake library reads as not connected lately.
        const val DORMANT_MEMORY = "{\"v\":1,\"lastConnected\":1}"
    }
}
