package rdx.works.peerdroid.data.iroh

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.fakes.FakeIrohConnector
import rdx.works.peerdroid.data.fakes.FakeIrohSessionFactory
import rdx.works.peerdroid.data.fakes.FakeNetworkChangeMonitor
import rdx.works.peerdroid.data.fakes.FakeTransportSettings
import rdx.works.peerdroid.data.fakes.transportProfile
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.MessageNotSentException
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class IrohPeerdroidLinkTest {

    private val factory = FakeIrohSessionFactory()
    private val settings = FakeTransportSettings(relayUrl = "https://relay.example.com")
    private val irohConnector = FakeIrohConnector()

    private val p2pLink = P2pLink.sample()
    private val connectionId = ConnectionIdHolder(p2pLink).id
    private val linkClient = PeerdroidLink.LinkClientExchangeInteraction(
        publicKey = PublicKey.Ed25519.sample(),
        signature = Signature.Ed25519.sample()
    )

    private fun TestScope.link(connectTimeout: Duration = 60.seconds) = IrohPeerdroidLink(
        sessionFactory = factory,
        irohConnector = irohConnector,
        settings = settings,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        closingScope = backgroundScope,
        connectTimeout = connectTimeout
    )

    private fun listener(onConnected: suspend (String) -> Result<Unit>) = object : PeerdroidLink.ConnectionListener {
        override suspend fun completeLinking(connectionId: String): Result<Unit> = onConnected(connectionId)
    }

    @Test
    fun `the link client message is sent once the connector extension is connected`() = runTest {
        val sut = link()
        var completedConnectionId: String? = null

        val result = async {
            sut.addConnection(
                p2pLink = p2pLink,
                p2pTransportProfile = transportProfile,
                connectionListener = listener { id ->
                    completedConnectionId = id
                    sut.sendMessage(id, linkClient)
                }
            )
        }
        runCurrent()

        val session = factory.sessions.single()
        assertEquals(ConnectionIdHolder(p2pLink.connectionPassword), session.connectionId)
        assertEquals("https://relay.example.com", session.relayUrl)
        assertNull(completedConnectionId)

        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTING))
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))

        assertTrue(result.await().isSuccess)
        assertEquals(connectionId, completedConnectionId)
        assertEquals(listOf(Json.encodeToString(linkClient)), session.sentMessages)
        assertTrue(session.isShutDown)
    }

    @Test
    fun `the linking session is read until it is shut down`() = runTest {
        val sut = link()
        var pendingReadsWhileCompleting = -1

        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener { id ->
                pendingReadsWhileCompleting = factory.sessions.single().pendingReads
                sut.sendMessage(id, linkClient)
            })
        }
        runCurrent()
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))

        assertTrue(result.await().isSuccess)
        assertEquals(1, pendingReadsWhileCompleting)
        assertTrue(session.isShutDown)
        assertEquals(0, session.pendingReads)
    }

    @Test
    fun `a message received while linking is handed to the incoming messages of the link`() = runTest {
        val sut = link()
        // The linking session stays open until the connector extension's message arrived on it.
        val messageSent = CompletableDeferred<Unit>()

        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener { id ->
                messageSent.await()
                sut.sendMessage(id, linkClient)
            })
        }
        runCurrent()
        val session = factory.sessions.single()
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        session.emit(IrohEvent.Message(DAPP_REQUEST))
        messageSent.complete(Unit)

        assertTrue(result.await().isSuccess)
        assertEquals(listOf(connectionId to DAPP_REQUEST), irohConnector.deliveredMessages)
    }

    @Test
    fun `a message received while linking reaches the incoming messages of the connector`() = runTest {
        val connector = IrohPeerdroidConnector(
            sessionFactory = factory,
            settings = settings,
            networkChangeMonitor = FakeNetworkChangeMonitor(),
            applicationScope = backgroundScope,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val received = mutableListOf<DataChannelWrapperEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            connector.dataChannelMessagesFromRemoteClients.toList(received)
        }
        val sut = IrohPeerdroidLink(
            sessionFactory = factory,
            irohConnector = connector,
            settings = settings,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            closingScope = backgroundScope
        )

        val messageSent = CompletableDeferred<Unit>()
        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener { id ->
                messageSent.await()
                sut.sendMessage(id, linkClient)
            })
        }
        runCurrent()
        val linkingSession = factory.sessions.single()
        linkingSession.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        linkingSession.emit(IrohEvent.Message(DAPP_REQUEST))
        messageSent.complete(Unit)
        assertTrue(result.await().isSuccess)
        runCurrent()

        assertEquals(
            listOf(DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(p2pLink), DAPP_REQUEST)),
            received
        )
    }

    @Test
    fun `the regular session of the link is paused while linking`() = runTest {
        val sut = link()

        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener { id -> sut.sendMessage(id, linkClient) })
        }
        runCurrent()
        assertEquals(listOf(connectionId), irohConnector.pausedIds)
        assertTrue(irohConnector.resumedIds.isEmpty())

        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        result.await()

        assertEquals(listOf(connectionId), irohConnector.resumedIds)
    }

    @Test
    fun `linking fails when the connector extension cannot be reached in time`() = runTest {
        val sut = link(connectTimeout = 5.seconds)
        var isLinkingCompleted = false

        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener {
                isLinkingCompleted = true
                Result.success(Unit)
            })
        }
        advanceTimeBy(6.seconds)

        assertTrue(result.await().exceptionOrNull() is TimeoutException)
        assertFalse(isLinkingCompleted)
        assertTrue(factory.sessions.single().isShutDown)
        assertEquals(listOf(connectionId), irohConnector.resumedIds)
    }

    @Test
    fun `linking fails when the session ends before connecting`() = runTest {
        val sut = link()

        val result = async { sut.addConnection(p2pLink, transportProfile, listener { Result.success(Unit) }) }
        runCurrent()
        factory.sessions.single().shutdownAndClose()

        assertTrue(result.await().isFailure)
    }

    @Test
    fun `linking fails when the session cannot start`() = runTest {
        factory.startError = IllegalStateException("invalid relay URL")
        val sut = link()

        val result = sut.addConnection(p2pLink, transportProfile, listener { Result.success(Unit) })

        assertTrue(result.isFailure)
        assertEquals(listOf(connectionId), irohConnector.resumedIds)
    }

    @Test
    fun `a failure to complete the linking is reported and the session is closed`() = runTest {
        val sut = link()
        val error = IllegalStateException("could not save the link")

        val result = async { sut.addConnection(p2pLink, transportProfile, listener { Result.failure(error) }) }
        runCurrent()
        factory.sessions.single().emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))

        assertEquals(error, result.await().exceptionOrNull())
        assertTrue(factory.sessions.single().isShutDown)
    }

    @Test
    fun `the linking is over once confirmed, while its session closes in the background with the link paused`() = runTest {
        val sut = link()

        val result = async {
            sut.addConnection(p2pLink, transportProfile, listener { id -> sut.sendMessage(id, linkClient) })
        }
        runCurrent()
        val session = factory.sessions.single()
        // Closing waits for the connection to drain.
        val drained = CompletableDeferred<Unit>()
        session.shutdownGate = drained
        session.emit(IrohEvent.StateChanged(IrohConnectionStatus.CONNECTED))
        runCurrent()

        assertTrue(result.isCompleted)
        assertTrue(result.await().isSuccess)
        assertTrue(session.isShutDown)
        assertTrue("the regular session waits for the close", irohConnector.resumedIds.isEmpty())

        drained.complete(Unit)
        runCurrent()

        assertEquals(listOf(connectionId), irohConnector.resumedIds)
    }

    @Test
    fun `the link client message cannot be sent outside of a linking`() = runTest {
        val sut = link()

        val result = sut.sendMessage(connectionId, linkClient)

        assertTrue(result.exceptionOrNull() is MessageNotSentException)
    }

    companion object {
        private const val DAPP_REQUEST = """{"interactionId":"1"}"""
    }
}
