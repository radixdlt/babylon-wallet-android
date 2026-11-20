package com.babylon.wallet.android.data.repository.p2plink

import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, UsesSampleValues::class)
class LinkRemovalNotifierTest {

    private val password = RadixConnectPassword.init(Exactly32Bytes.init(ByteArray(32) { it.toByte() }.toBagOfBytes()))
    private val retiredLink = RetiredP2PLink(connectionPassword = password, retiredAtMillis = NOW)
    private val connected = MutableStateFlow(false)
    private val sendResults = ArrayDeque<Result<Unit>>()
    private val sent = mutableListOf<Pair<String, String>>()
    private val retiredLinks = MutableStateFlow(listOf(retiredLink))

    private val peerdroidClient = mockk<PeerdroidClient>(relaxUnitFun = true) {
        coEvery { connect(any(), any()) } returns Result.success(Unit)
        every { isConnectionOpen(retiredLink.connectorId) } returns connected
        coEvery { sendMessage(any<String>(), any<String>()) } answers {
            sent += firstArg<String>() to secondArg<String>()
            sendResults.removeFirstOrNull() ?: Result.success(Unit)
        }
    }
    private val retiredP2PLinksRepository = mockk<RetiredP2PLinksRepository>(relaxUnitFun = true) {
        every { observeRetiredP2PLinks() } returns retiredLinks
    }
    private val p2PLinksRepository = mockk<P2PLinksRepository>()
    private val irohCapable = MutableStateFlow(setOf(retiredLink.connectorId))
    private val transportSettings = mockk<RadixConnectTransportSettings> {
        every { irohCapableConnectionIds } returns irohCapable
    }

    private val sut = LinkRemovalNotifier(
        peerdroidClient = peerdroidClient,
        p2PLinksRepository = p2PLinksRepository,
        retiredP2PLinksRepository = retiredP2PLinksRepository,
        transportSettings = transportSettings,
        json = Json { ignoreUnknownKeys = true }
    )

    @Test
    fun `opens the removed link, tells the connector extension once connected, then deletes the connector`() = runTest {
        val delivery = launch { sut.deliver(retiredLink, P2pTransportProfile.sample(), nowMillis = NOW) }
        runCurrent()
        coVerify { peerdroidClient.connect(any(), password) }
        assertTrue(sent.isEmpty())

        connected.value = true
        runCurrent()

        assertEquals(listOf(retiredLink.connectorId to """{"discriminator":"linkRemoved"}"""), sent)
        // The connection stays up a moment, so that the message arrives before it is closed.
        coVerify(exactly = 0) { peerdroidClient.deleteLink(any()) }
        advanceTimeBy(3.seconds.inWholeMilliseconds + 1)
        runCurrent()
        coVerify { peerdroidClient.deleteLink(password) }
        coVerify { retiredP2PLinksRepository.forget(retiredLink.connectorId) }
        assertTrue(delivery.isCompleted)
    }

    @Test
    fun `a connector extension not known to support iroh is not told, and its connector is deleted at once`() = runTest {
        irohCapable.value = emptySet()
        connected.value = true

        sut.deliver(retiredLink, P2pTransportProfile.sample(), nowMillis = NOW)

        assertTrue(sent.isEmpty())
        coVerify(exactly = 0) { peerdroidClient.connect(any(), any()) }
        coVerify { peerdroidClient.deleteLink(password) }
        coVerify { retiredP2PLinksRepository.forget(retiredLink.connectorId) }
    }

    @Test
    fun `tries again while the notice is not sent, and keeps the connector meanwhile`() = runTest {
        sut.retryDelay = 5.seconds
        sendResults += Result.failure(IllegalStateException("not sent"))
        connected.value = true

        val delivery = launch { sut.deliver(retiredLink, P2pTransportProfile.sample(), nowMillis = NOW) }
        runCurrent()
        assertEquals(1, sent.size)
        coVerify(exactly = 0) { peerdroidClient.deleteLink(any()) }

        advanceTimeBy(5.seconds.inWholeMilliseconds + 1)
        runCurrent()
        assertEquals(2, sent.size)
        advanceTimeBy(3.seconds.inWholeMilliseconds + 1)
        runCurrent()

        coVerify { peerdroidClient.deleteLink(password) }
        assertTrue(delivery.isCompleted)
    }

    @Test
    fun `an expired notice deletes the connector without opening the link`() = runTest {
        sut.deliver(retiredLink, P2pTransportProfile.sample(), nowMillis = NOW + 7.days.inWholeMilliseconds)

        coVerify(exactly = 0) { peerdroidClient.connect(any(), any()) }
        coVerify { peerdroidClient.deleteLink(password) }
        coVerify { retiredP2PLinksRepository.forget(retiredLink.connectorId) }
    }

    @Test
    fun `a notice that expires while it waits for the connector extension deletes the connector untold`() = runTest {
        val delivery = launch { sut.deliver(retiredLink, P2pTransportProfile.sample(), nowMillis = NOW + 6.days.inWholeMilliseconds) }
        runCurrent()
        coVerify(exactly = 0) { peerdroidClient.deleteLink(any()) }

        advanceTimeBy(1.days.inWholeMilliseconds + 1)
        runCurrent()

        assertTrue(sent.isEmpty())
        coVerify { peerdroidClient.deleteLink(password) }
        coVerify { retiredP2PLinksRepository.forget(retiredLink.connectorId) }
        assertTrue(delivery.isCompleted)
    }

    @Test
    fun `a removal by the connector extension ends the notice of a link removed here too, untold`() = runTest {
        assertNull(sut.onLinkRemovedByConnector(retiredLink.connectorId))

        coVerify { peerdroidClient.deleteLink(password) }
        coVerify { retiredP2PLinksRepository.forget(retiredLink.connectorId) }
        coVerify(exactly = 0) { p2PLinksRepository.removeP2PLinkRemovedByConnector(any()) }
    }

    @Test
    fun `a removal by the connector extension removes its link here, without telling it back`() = runTest {
        retiredLinks.value = emptyList()
        val link = P2pLink.sample()
        coEvery { p2PLinksRepository.removeP2PLinkRemovedByConnector("abc") } returns link

        assertEquals(link, sut.onLinkRemovedByConnector("abc"))
        assertTrue(sent.isEmpty())
        coVerify(exactly = 0) { retiredP2PLinksRepository.retire(any(), any()) }
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
