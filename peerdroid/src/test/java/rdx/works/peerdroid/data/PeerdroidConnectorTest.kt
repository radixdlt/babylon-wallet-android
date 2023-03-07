package rdx.works.peerdroid.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.helpers.Result
import kotlin.test.Test

const val ENCRYPTION_KEY = "acc9ff51f550e1f4e7cc746b46e4686ebcc17fdcf2d42d9e96ebb2dbbb04aa60"

@OptIn(ExperimentalCoroutinesApi::class)
class PeerdroidConnectorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeWebSocketClient: FakeWebSocketClient
    private lateinit var fakeWebRtcManager: FakeWebRtcManager

    private lateinit var subject: PeerdroidConnector

    @Test
    fun `add connection`() {
        fakeWebRtcManager = FakeWebRtcManager(testDispatcher)
        fakeWebSocketClient = FakeWebSocketClient(testDispatcher)

        subject = PeerdroidConnectorImpl(fakeWebRtcManager, fakeWebSocketClient, testScope, testDispatcher)

        testScope.runTest {
            val result = subject.addConnection(ENCRYPTION_KEY.decodeHex().toByteArray())

            assert(result is Result.Success)
        }
    }
}
