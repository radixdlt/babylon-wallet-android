package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import rdx.works.peerdroid.domain.TransportDiagnostics
import kotlin.time.Duration.Companion.seconds

// The connections refresh every 2 seconds while collected: these tests never wait until idle.
@OptIn(ExperimentalCoroutinesApi::class)
class LinkedConnectorsViewModelTest : StateViewModelTest<LinkedConnectorsViewModel>() {

    private val p2pLinksRepository = mockk<P2PLinksRepository>()
    private val links = listOf(P2pLink.sample(), P2pLink.sample.other())
    private var diagnostics = emptyList<LinkDiagnostics>()

    private val radixConnectDiagnostics = object : RadixConnectDiagnostics {
        override suspend fun linkDiagnostics(): List<LinkDiagnostics> = diagnostics
    }

    override fun initVM(): LinkedConnectorsViewModel = LinkedConnectorsViewModel(
        p2pLinksRepository = p2pLinksRepository,
        radixConnectDiagnostics = radixConnectDiagnostics,
        savedStateHandle = SavedStateHandle(mapOf(ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN to false))
    )

    @Before
    override fun setUp() {
        super.setUp()
        every { p2pLinksRepository.observeP2PLinks() } returns flowOf(P2pLinks(links))
        coEvery { p2pLinksRepository.getP2PLinks() } returns P2pLinks(links)
    }

    private fun diagnostics(link: P2pLink, vararg transports: TransportDiagnostics) = LinkDiagnostics(
        connectionId = link.clientID().hex,
        status = null,
        transports = transports.toList()
    )

    private fun webRtc(status: PeerConnectionStatus?) = TransportDiagnostics.WebRtc(status = status, lastError = null, signalingServer = null)

    private fun iroh(status: PeerConnectionStatus?) = TransportDiagnostics.Iroh(status = status, lastError = null, session = null)

    @Test
    fun `each connector shows the transport that carries it`() = runTest {
        diagnostics = listOf(
            diagnostics(links[0], iroh(PeerConnectionStatus.OPEN), webRtc(PeerConnectionStatus.OPEN)),
            diagnostics(links[1], webRtc(PeerConnectionStatus.OPEN), iroh(PeerConnectionStatus.CLOSED))
        )
        val vm = vm.value
        assertNull(vm.connectorConnections.value)

        backgroundScope.launch { vm.connectorConnections.collect {} }
        runCurrent()

        assertEquals(
            mapOf(links[0].id to ConnectorConnection.IROH, links[1].id to ConnectorConnection.WEBRTC),
            vm.connectorConnections.value
        )
    }

    @Test
    fun `connectors without an open transport are not connected`() = runTest {
        // the second link is not even being connected
        diagnostics = listOf(diagnostics(links[0], webRtc(PeerConnectionStatus.CONNECTING), iroh(PeerConnectionStatus.CLOSED)))
        val vm = vm.value

        backgroundScope.launch { vm.connectorConnections.collect {} }
        runCurrent()

        assertEquals(
            mapOf(links[0].id to ConnectorConnection.NOT_CONNECTED, links[1].id to ConnectorConnection.NOT_CONNECTED),
            vm.connectorConnections.value
        )
    }

    @Test
    fun `the connections refresh while shown`() = runTest {
        val vm = vm.value
        backgroundScope.launch { vm.connectorConnections.collect {} }
        runCurrent()
        assertEquals(ConnectorConnection.NOT_CONNECTED, vm.connectorConnections.value?.get(links[0].id))

        diagnostics = listOf(diagnostics(links[0], iroh(PeerConnectionStatus.OPEN)))
        advanceTimeBy(2.seconds)
        runCurrent()

        assertEquals(ConnectorConnection.IROH, vm.connectorConnections.value?.get(links[0].id))
    }
}
