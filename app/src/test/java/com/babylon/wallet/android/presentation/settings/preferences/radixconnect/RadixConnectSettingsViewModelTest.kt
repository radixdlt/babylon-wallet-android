package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelaysRepositoryImpl
import com.babylon.wallet.android.fakes.FakeIrohRelayConfig
import com.babylon.wallet.android.fakes.FakeRadixConnectPreferences
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.P2pLinks
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.domain.SavedIrohRelays
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import rdx.works.peerdroid.domain.TransportDiagnostics
import rdx.works.profile.domain.GetProfileUseCase

// The diagnostics refresh every 2 seconds while collected: these tests never wait until idle.
@OptIn(ExperimentalCoroutinesApi::class)
class RadixConnectSettingsViewModelTest : StateViewModelTest<RadixConnectSettingsViewModel>() {

    private val preferences = FakeRadixConnectPreferences()
    private val relayConfig = FakeIrohRelayConfig()
    private val p2pLinksRepository = mockk<P2PLinksRepository>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()

    private val link = P2pLink.sample()
    private val connectionId = link.clientID().hex

    private val diagnostics = object : RadixConnectDiagnostics {
        override suspend fun linkDiagnostics(): List<LinkDiagnostics> = listOf(
            LinkDiagnostics(
                connectionId = connectionId,
                status = PeerConnectionStatus.OPEN,
                transports = listOf(
                    TransportDiagnostics.WebRtc(
                        status = PeerConnectionStatus.OPEN,
                        lastError = null,
                        signalingServer = "wss://signaling.example.com"
                    )
                )
            )
        )
    }

    override fun initVM(): RadixConnectSettingsViewModel = RadixConnectSettingsViewModel(
        radixConnectPreferences = preferences,
        irohRelaysRepository = IrohRelaysRepositoryImpl(
            radixConnectPreferences = preferences,
            irohRelayConfig = relayConfig,
            defaultDispatcher = coroutineRule.dispatcher
        ),
        irohRelayConfig = relayConfig,
        radixConnectDiagnostics = diagnostics,
        p2pLinksRepository = p2pLinksRepository,
        getProfileUseCase = getProfileUseCase,
        defaultDispatcher = coroutineRule.dispatcher
    )

    @Before
    override fun setUp() {
        super.setUp()
        every { getProfileUseCase.flow } returns flowOf(Profile.sample())
        coEvery { p2pLinksRepository.getP2PLinks() } returns P2pLinks(link)
    }

    @Test
    fun `the connection method and the relay servers are shown`() = runTest {
        preferences.transportState.value = RadixConnectTransport.WEBRTC
        preferences.irohRelaysState.value = SavedIrohRelays(current = IrohRelay("https://relay.example.com"))

        val vm = vm.value
        runCurrent()

        val state = vm.state.value
        assertEquals(RadixConnectTransport.WEBRTC, state.transport)
        assertEquals(IrohRelayServer.Custom("https://relay.example.com"), state.relayServers?.current)
        assertEquals(listOf(IrohRelayServer.Public), state.relayServers?.others)
        assertEquals(FakeIrohRelayConfig.PUBLIC_RELAYS, state.publicRelayUrls)
    }

    @Test
    fun `the selected connection method is stored`() = runTest {
        val vm = vm.value
        runCurrent()

        vm.onTransportSelected(RadixConnectTransport.IROH)
        runCurrent()

        assertEquals(RadixConnectTransport.IROH, preferences.transportState.value)
        assertEquals(RadixConnectTransport.IROH, vm.state.value.transport)
    }

    @Test
    fun `in debug builds the diagnostics of every connector are shown with its name`() = runTest {
        val vm = vm.value
        // Like the screen: the diagnostics refresh only while collected.
        backgroundScope.launch { vm.connectorDiagnostics.collect {} }
        runCurrent()

        val connectors = requireNotNull(vm.connectorDiagnostics.value)
        assertEquals(listOf(link.displayName), connectors.map { it.name })
        assertEquals(PeerConnectionStatus.OPEN, connectors.single().diagnostics.status)
    }

    @Test
    fun `the copied diagnostics name the connector but hold no full connection id`() = runTest {
        val vm = vm.value
        backgroundScope.launch { vm.connectorDiagnostics.collect {} }
        runCurrent()

        val report = vm.diagnosticsReport()

        assertTrue(report.contains(link.displayName))
        assertTrue(report.contains(connectionId.take(8)))
        assertFalse(report.contains(connectionId))
    }

    @Test
    fun `the copied diagnostics never hold the password of a link`() = runTest {
        val vm = vm.value
        backgroundScope.launch { vm.connectorDiagnostics.collect {} }
        runCurrent()

        val report = vm.diagnosticsReport()

        assertTrue(report.contains(link.displayName))
        assertFalse(report.contains(link.connectionPassword.value.hex, ignoreCase = true))
    }
}
