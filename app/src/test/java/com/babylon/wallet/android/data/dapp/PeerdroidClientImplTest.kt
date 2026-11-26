package com.babylon.wallet.android.data.dapp

import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.PeerConnectionStatus
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class PeerdroidClientImplTest {

    private val connectionId = "1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b"
    private val incoming = MutableSharedFlow<DataChannelWrapperEvent>()
    private val logged = mutableListOf<String>()
    private val sent = mutableListOf<String>()

    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logged += message
        }
    }

    private val connector = object : PeerdroidConnector {
        override val anyChannelConnected: Flow<Boolean> = flowOf(true)
        override val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>> = flowOf(emptyMap())
        override val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent> = incoming

        override suspend fun connectToConnectorExtension(
            p2pTransportProfile: P2pTransportProfile,
            encryptionKey: RadixConnectPassword
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteConnector(connectionId: ConnectionIdHolder) = Unit
        override fun terminateConnectionToConnectorExtension() = Unit
        override suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit> {
            sent += message
            return Result.success(Unit)
        }
        override suspend fun sendDataChannelMessageToAllRemoteClients(message: String) = Result.success(Unit)
    }

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uproot(tree)
    }

    @Test
    fun `a message that is neither a dApp request nor a ledger response is rejected without logging its contents`() = runTest {
        val sut = PeerdroidClientImpl(
            peerdroidConnector = connector,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            json = Json { ignoreUnknownKeys = true }
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.listenForIncomingRequests().collect {} }
        runCurrent()
        val message = """{"interactionId":"1","persona":{"label":"Alice","seedPhrase":"zoo zoo zoo"}}"""

        incoming.emit(DataChannelWrapperEvent.MessageFromRemoteConnectionId(ConnectionIdHolder(connectionId), message))
        runCurrent()

        assertTrue(logged.toString(), logged.any { it.startsWith("🤖 link=1a2b3c4d rejected a ") })
        assertTrue(logged.none { "Alice" in it || "zoo" in it || connectionId in it })
    }
}
