package rdx.works.peerdroid.data

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.data.fakes.FakePeerdroidLink
import rdx.works.peerdroid.data.fakes.FakeTransportSettings
import rdx.works.peerdroid.data.fakes.RecordingTree
import rdx.works.peerdroid.data.fakes.transportProfile
import rdx.works.peerdroid.domain.ConnectionIdHolder
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

@OptIn(UsesSampleValues::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompositePeerdroidLinkTest {

    @get:Rule
    val logs = RecordingTree()

    private val p2pLink = P2pLink.sample()
    private val connectionId = ConnectionIdHolder(p2pLink).id
    private val linkClient = PeerdroidLink.LinkClientExchangeInteraction(
        publicKey = PublicKey.Ed25519.sample(),
        signature = Signature.Ed25519.sample()
    )
    private val settings = FakeTransportSettings()

    private var completeLinkingCount = 0

    // What the use case does: send the link client message, then save the link.
    private fun CompositePeerdroidLink.sendingListener(
        onLinkSaved: () -> Result<Unit> = { Result.success(Unit) }
    ) = object : PeerdroidLink.ConnectionListener {
        override suspend fun completeLinking(connectionId: String): Result<Unit> {
            completeLinkingCount++
            return sendMessage(connectionId, linkClient).mapCatching { onLinkSaved().getOrThrow() }
        }
    }

    private fun TestScope.composite(
        transport: RadixConnectTransport,
        webRtcLink: Lazy<PeerdroidLink>,
        irohLink: Lazy<PeerdroidLink>,
        irohCapable: Set<String> = emptySet()
    ): CompositePeerdroidLink {
        settings.transportState.value = transport
        settings.irohCapableState.value = irohCapable
        return CompositePeerdroidLink(
            links = mapOf(PeerdroidTransport.WEBRTC to webRtcLink, PeerdroidTransport.IROH to irohLink),
            settings = settings,
            learningScope = backgroundScope,
            monotonicMillis = { testScheduler.currentTime }
        )
    }

    // A transport that connects after [after] and reports it like WebRTC does: the call is not cancellable (the
    // connector extension may already have the link client message), and its result is kept for the assertions.
    private class NonCancellableTransport(after: kotlin.time.Duration, connectionId: String) {
        var completion: Result<Unit>? = null
        val link = FakePeerdroidLink { listener ->
            delay(after)
            withContext(NonCancellable) { listener.completeLinking(connectionId).also { completion = it } }
        }
    }

    private val linkingLines: List<String>
        get() = logs.lines.filter { it.tag == RadixConnectLog.LINKING_TAG }.map { it.message }

    @Test
    fun `in WebRTC mode only the WebRTC link is created and used`() = runTest {
        val webRtc = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        var isIrohCreated = false
        val sut = composite(
            transport = RadixConnectTransport.WEBRTC,
            webRtcLink = lazyOf(webRtc),
            irohLink = lazy {
                isIrohCreated = true
                FakePeerdroidLink { Result.success(Unit) }
            }
        )

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertFalse(isIrohCreated)
        assertEquals(listOf(connectionId), webRtc.sentMessages)
        assertTrue(settings.remembered.isEmpty())
    }

    @Test
    fun `in iroh mode only the iroh link is used, and the link is remembered as supporting iroh`() = runTest {
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val webRtc = FakePeerdroidLink { Result.success(Unit) }
        val sut = composite(RadixConnectTransport.IROH, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(0, webRtc.addConnectionCount)
        assertEquals(listOf(connectionId), iroh.sentMessages)
        assertEquals(setOf(connectionId), settings.irohCapableState.value)
    }

    @Test
    fun `in automatic mode iroh connecting first wins and the link supports iroh before it is saved`() = runTest {
        val webRtc = FakePeerdroidLink { awaitCancellation() }
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))
        var supportsIrohWhenSaved = false

        val result = sut.addConnection(
            p2pLink,
            transportProfile,
            sut.sendingListener(onLinkSaved = {
                supportsIrohWhenSaved = connectionId in settings.irohCapableState.value
                Result.success(Unit)
            })
        )

        assertTrue(result.isSuccess)
        assertTrue(webRtc.wasCancelledOrNeverStarted)
        assertEquals(listOf(connectionId), iroh.sentMessages)
        assertTrue(webRtc.sentMessages.isEmpty())
        assertEquals(1, completeLinkingCount)
        assertTrue(supportsIrohWhenSaved)
    }

    @Test
    fun `in automatic mode WebRTC connecting first wins, and iroh goes on after the linking without anything learned yet`() =
        runTest {
            val webRtc = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
            val iroh = FakePeerdroidLink { awaitCancellation() }
            val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

            val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

            assertTrue(result.isSuccess)
            assertEquals(1, iroh.addConnectionCount)
            assertFalse(iroh.wasCancelled)
            assertEquals(listOf(connectionId), webRtc.sentMessages)
            assertTrue(settings.irohCapableState.value.isEmpty())
        }

    @Test
    fun `an iroh attempt that connects after WebRTC won the linking is remembered as supporting iroh`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        var irohCompletion: Result<Unit>? = null
        val iroh = FakePeerdroidLink { listener ->
            delay(20.seconds)
            listener.completeLinking(connectionId).also { irohCompletion = it }
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        // The linking does not wait for iroh: WebRTC completed it at the end of the grace period.
        assertTrue(result.isSuccess)
        assertEquals(1_000 + CompositePeerdroidLink.PREFERRED_TRANSPORT_GRACE.inWholeMilliseconds, testScheduler.currentTime)
        assertTrue(settings.irohCapableState.value.isEmpty())

        advanceTimeBy(20.seconds)
        runCurrent()

        assertTrue(irohCompletion?.exceptionOrNull() is LinkingSupersededException)
        assertEquals(1, completeLinkingCount)
        assertTrue(iroh.sentMessages.isEmpty())
        assertEquals(setOf(connectionId), settings.irohCapableState.value)
        assertTrue(linkingLines.any { it.endsWith("iroh support learned: iroh connected after the linking") })
    }

    @Test
    fun `the iroh attempt going on after the linking is cancelled when the link could not be saved`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { awaitCancellation() }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(
            p2pLink,
            transportProfile,
            sut.sendingListener(onLinkSaved = { Result.failure(IllegalStateException("not saved")) })
        )
        runCurrent()

        assertTrue(result.isFailure)
        assertTrue(iroh.wasCancelled)
        assertTrue(settings.irohCapableState.value.isEmpty())
    }

    @Test
    fun `a link whose iroh attempt also connected is remembered as supporting iroh`() = runTest {
        val irohConnected = CompletableDeferred<Unit>()
        // iroh connects at the same time as WebRTC wins, too late to be cancelled.
        val iroh = FakePeerdroidLink { listener ->
            withContext(NonCancellable) {
                irohConnected.await()
                listener.completeLinking(connectionId)
            }
        }
        val webRtc = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(
            p2pLink,
            transportProfile,
            sut.sendingListener(onLinkSaved = {
                irohConnected.complete(Unit)
                Result.success(Unit)
            })
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(connectionId), webRtc.sentMessages)
        assertEquals(1, completeLinkingCount)
        // Learned as soon as the iroh attempt ends, which the linking does not wait for.
        runCurrent()
        assertEquals(setOf(connectionId), settings.irohCapableState.value)
    }

    @Test
    fun `a new link that could not be saved after iroh won is not remembered`() = runTest {
        val webRtc = FakePeerdroidLink { awaitCancellation() }
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(
            p2pLink,
            transportProfile,
            sut.sendingListener(onLinkSaved = { Result.failure(IllegalStateException("could not save the link")) })
        )

        assertTrue(result.isFailure)
        assertTrue(settings.irohCapableState.value.isEmpty())
    }

    @Test
    fun `a failed re-link keeps what was known about the link`() = runTest {
        val webRtc = FakePeerdroidLink { awaitCancellation() }
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh), irohCapable = setOf(connectionId))

        val result = sut.addConnection(
            p2pLink,
            transportProfile,
            sut.sendingListener(onLinkSaved = { Result.failure(IllegalStateException("could not save the link")) })
        )

        assertTrue(result.isFailure)
        assertEquals(setOf(connectionId), settings.irohCapableState.value)
    }

    @Test
    fun `only one transport completes the linking`() = runTest {
        val webRtc = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1, completeLinkingCount)
        assertEquals(1, webRtc.sentMessages.size + iroh.sentMessages.size)
    }

    @Test
    fun `when one transport fails the other one can still complete the linking`() = runTest {
        val irohFailed = CompletableDeferred<Unit>()
        val iroh = FakePeerdroidLink {
            irohFailed.complete(Unit)
            Result.failure(TimeoutException("could not reach the connector extension over iroh"))
        }
        val webRtc = FakePeerdroidLink { listener ->
            irohFailed.await()
            listener.completeLinking(connectionId)
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(listOf(connectionId), webRtc.sentMessages)
    }

    @Test
    fun `the linking fails when every transport fails`() = runTest {
        val webRtc = FakePeerdroidLink { Result.failure(IllegalStateException("webrtc")) }
        val iroh = FakePeerdroidLink { Result.failure(IllegalStateException("iroh")) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isFailure)
        assertEquals(0, completeLinkingCount)
    }

    @Test
    fun `the winner's failure to complete the linking is reported`() = runTest {
        val error = IllegalStateException("could not send the link client message")
        val webRtc = FakePeerdroidLink { awaitCancellation() }
        val iroh = FakePeerdroidLink { listener -> listener.completeLinking(connectionId) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))
        val failingListener = object : PeerdroidLink.ConnectionListener {
            override suspend fun completeLinking(connectionId: String): Result<Unit> = Result.failure(error)
        }

        val result = sut.addConnection(p2pLink, transportProfile, failingListener)

        assertEquals(error, result.exceptionOrNull())
        assertTrue(webRtc.wasCancelledOrNeverStarted)
    }

    // ---- iroh is preferred: the grace period given to it when WebRTC connects first

    @Test
    fun `iroh connecting first wins at once`() = runTest {
        val webRtc = FakePeerdroidLink { awaitCancellation() }
        val iroh = FakePeerdroidLink { listener ->
            delay(1.seconds)
            listener.completeLinking(connectionId)
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1_000, testScheduler.currentTime)
        assertEquals(1, completeLinkingCount)
        assertEquals(listOf(connectionId), iroh.sentMessages)
        assertTrue(webRtc.wasCancelledOrNeverStarted)
        assertTrue(linkingLines.any { it.endsWith("linking won by iroh after 1000 ms: it connected first") })
    }

    @Test
    fun `WebRTC connecting first waits for iroh, which wins when it connects within the grace period`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { listener ->
            delay(3.seconds)
            listener.completeLinking(connectionId)
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(3_000, testScheduler.currentTime)
        assertEquals(1, completeLinkingCount)
        assertEquals(listOf(connectionId), iroh.sentMessages)
        assertTrue(webRtc.link.sentMessages.isEmpty())
        // WebRTC is told to close without completing the linking.
        assertTrue(webRtc.completion?.exceptionOrNull() is LinkingSupersededException)
        assertEquals(setOf(connectionId), settings.irohCapableState.value)
        assertTrue(linkingLines.any { it.endsWith("webrtc connected first, waiting up to 5000 ms for [iroh]") })
        assertTrue(
            linkingLines.any {
                it.endsWith("linking won by iroh after 3000 ms: it connected 2000 ms into the 5000 ms grace period given to it by webrtc")
            }
        )
    }

    @Test
    fun `WebRTC connecting first wins once the grace period is over when iroh does not connect`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { awaitCancellation() }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1_000 + CompositePeerdroidLink.PREFERRED_TRANSPORT_GRACE.inWholeMilliseconds, testScheduler.currentTime)
        assertEquals(1, completeLinkingCount)
        assertEquals(listOf(connectionId), webRtc.link.sentMessages)
        // It goes on after the linking, to find out whether the connector extension supports iroh.
        assertFalse(iroh.wasCancelled)
        assertTrue(settings.irohCapableState.value.isEmpty())
        assertTrue(linkingLines.any { it.endsWith("linking won by webrtc after 6000 ms: [iroh] did not connect within the 5000 ms grace period") })
    }

    @Test
    fun `WebRTC wins at once when the iroh attempt fails during the grace period`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink {
            delay(2.5.seconds)
            Result.failure(TimeoutException("could not reach the connector extension over iroh"))
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(2_500, testScheduler.currentTime)
        assertEquals(1, completeLinkingCount)
        assertEquals(listOf(connectionId), webRtc.link.sentMessages)
        assertTrue(linkingLines.any { it.endsWith("linking won by webrtc after 2500 ms: the [iroh] attempt ended 1500 ms into the 5000 ms grace period") })
    }

    @Test
    fun `WebRTC does not wait when the iroh attempt failed already`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink {
            delay(0.5.seconds)
            Result.failure(IllegalStateException("the native library is not available"))
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1_000, testScheduler.currentTime)
        assertEquals(listOf(connectionId), webRtc.link.sentMessages)
        assertTrue(linkingLines.none { "waiting up to" in it })
    }

    @Test
    fun `the linking is completed exactly once when iroh connects as the grace period ends`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { listener ->
            delay(1.seconds + CompositePeerdroidLink.PREFERRED_TRANSPORT_GRACE)
            withContext(NonCancellable) { listener.completeLinking(connectionId) }
        }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1, completeLinkingCount)
        assertEquals(1, webRtc.link.sentMessages.size + iroh.sentMessages.size)
    }

    @Test
    fun `nothing completes the linking when the caller gives up during the grace period`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { awaitCancellation() }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        val linking = async { sut.addConnection(p2pLink, transportProfile, sut.sendingListener()) }
        advanceTimeBy(2.seconds)
        linking.cancel()
        runCurrent()

        assertEquals(0, completeLinkingCount)
        assertTrue(webRtc.completion?.exceptionOrNull() is LinkingSupersededException)
        assertTrue(webRtc.link.sentMessages.isEmpty())
        // Nothing goes on after a linking given up.
        assertTrue(iroh.wasCancelled)
    }

    @Test
    fun `in WebRTC mode WebRTC completes the linking without waiting`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val sut = composite(RadixConnectTransport.WEBRTC, lazyOf(webRtc.link), lazy { error("iroh is not used") })

        val result = sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        assertTrue(result.isSuccess)
        assertEquals(1_000, testScheduler.currentTime)
        assertEquals(listOf(connectionId), webRtc.link.sentMessages)
    }

    @Test
    fun `the linking logs never hold the password nor the full connection id`() = runTest {
        val webRtc = NonCancellableTransport(after = 1.seconds, connectionId = connectionId)
        val iroh = FakePeerdroidLink { Result.failure(TimeoutException("could not reach the connector extension over iroh")) }
        val sut = composite(RadixConnectTransport.AUTOMATIC, lazyOf(webRtc.link), lazyOf(iroh))

        sut.addConnection(p2pLink, transportProfile, sut.sendingListener())

        val password = p2pLink.connectionPassword.value.hex
        assertTrue(logs.lines.isNotEmpty())
        assertNull(logs.lines.firstOrNull { password in it.message || connectionId in it.message })
        assertTrue(linkingLines.all { "link=${connectionId.take(8)}" in it })
    }
}
