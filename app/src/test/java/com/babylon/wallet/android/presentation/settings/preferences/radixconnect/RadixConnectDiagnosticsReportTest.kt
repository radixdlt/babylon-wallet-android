package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.IrohPath
import rdx.works.peerdroid.domain.IrohSessionDiagnostics
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.LinkEvent
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics
import java.util.Base64

class RadixConnectDiagnosticsReportTest {

    private val connectionId = "1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b"

    private val connector = ConnectorDiagnostics(
        name = "Chrome on my laptop",
        diagnostics = LinkDiagnostics(
            connectionId = connectionId,
            status = PeerConnectionStatus.OPEN,
            transports = listOf(
                TransportDiagnostics.Iroh(
                    status = PeerConnectionStatus.OPEN,
                    lastError = null,
                    session = IrohSessionDiagnostics(
                        relayUrl = "https://euc1-1.relay.n0.iroh.link./",
                        usesPublicRelays = true,
                        isRelayConnected = true,
                        relayError = null,
                        path = IrohPath.DIRECT,
                        rttMs = 42,
                        failedAttempts = 0,
                        lastError = null,
                        connectedForMs = 12_345,
                        summary = "wallet 4d99dc649e → efcc89cfbf state=connected path=direct rtt=42ms"
                    )
                ),
                TransportDiagnostics.WebRtc(
                    status = PeerConnectionStatus.CLOSED,
                    lastError = "peer connection disconnected",
                    signalingServer = "wss://signaling.example.com"
                )
            )
        )
    )

    private fun report(
        connectors: List<ConnectorDiagnostics> = listOf(connector),
        relayServer: IrohRelayServer = IrohRelayServer.Public
    ) = RadixConnectDiagnosticsReport.build(
        appVersion = "1.22.2 (95)",
        transport = RadixConnectTransport.AUTOMATIC,
        relayServer = relayServer,
        connectors = connectors,
        nowMillis = NOW
    )

    @Test
    fun `the report lists the settings and every transport of every connector`() {
        assertEquals(
            """
            Radix Connect diagnostics
            App version: 1.22.2 (95)
            Connection method: automatic
            iroh relay server: public (closest, n0)
            Connectors: 1

            "Chrome on my laptop" (link 1a2b3c4d): open
              iroh link open; this wallet's relay public, https://euc1-1.relay.n0.iroh.link/ (reachable); path direct; RTT 42 ms; failed attempts 0; connected for 12 s; last error: none
              WebRTC link closed; signaling server wss://signaling.example.com; last error: peer connection disconnected
            """.trimIndent(),
            report()
        )
    }

    @Test
    fun `a custom relay is reported with its URL`() {
        val customConnector = connector.copy(
            diagnostics = connector.diagnostics.copy(
                transports = listOf(
                    TransportDiagnostics.Iroh(
                        status = PeerConnectionStatus.CLOSED,
                        lastError = "dial: timed out",
                        session = (connector.diagnostics.transports.first() as TransportDiagnostics.Iroh).session?.copy(
                            relayUrl = "https://relay.example.com/",
                            usesPublicRelays = false,
                            isRelayConnected = false,
                            path = null,
                            rttMs = null,
                            failedAttempts = 3,
                            connectedForMs = null
                        )
                    )
                )
            )
        )

        val report = report(listOf(customConnector), relayServer = IrohRelayServer.Custom("https://relay.example.com/"))

        assertEquals(
            listOf(
                "iroh relay server: https://relay.example.com/",
                "  iroh link closed; this wallet's relay https://relay.example.com/ (NOT reachable); failed attempts 3; " +
                    "last error: dial: timed out"
            ),
            report.lines().filter { it.contains("relay") }
        )
    }

    @Test
    fun `the report holds only the short form of the connection id`() {
        assertFalse(report().contains(connectionId))
    }

    @Test
    fun `a connector without transport is reported as such`() {
        val idle = connector.copy(diagnostics = connector.diagnostics.copy(status = null, transports = emptyList()))

        assertEquals(
            listOf("\"Chrome on my laptop\" (link 1a2b3c4d): not running", "  no transport"),
            report(listOf(idle)).lines().takeLast(2)
        )
    }

    @Test
    fun `the last few events of a connector are listed, oldest first, with their age`() {
        val events = (1..10).map { index ->
            LinkEvent(atMillis = NOW - (10 - index) * 1_000L, source = "link", description = "event $index")
        }
        val withEvents = connector.copy(diagnostics = connector.diagnostics.copy(recentEvents = events))

        val lines = report(listOf(withEvents)).lines()

        assertEquals(
            listOf(
                "  recent events:",
                "    7 s ago: link: event 3",
                "    6 s ago: link: event 4",
                "    5 s ago: link: event 5",
                "    4 s ago: link: event 6",
                "    3 s ago: link: event 7",
                "    2 s ago: link: event 8",
                "    1 s ago: link: event 9",
                "    0 s ago: link: event 10"
            ),
            lines.takeLast(9)
        )
    }

    @Test
    fun `older and repeated events are described as such`() {
        val events = listOf(
            LinkEvent(atMillis = NOW - 7_200_000, source = "webrtc", description = "peer connection failed"),
            LinkEvent(atMillis = NOW - 125_000, source = "iroh", description = "connected → disconnected", count = 3)
        )
        val withEvents = connector.copy(diagnostics = connector.diagnostics.copy(recentEvents = events))

        assertEquals(
            listOf("    2 h ago: webrtc: peer connection failed", "    2 min ago: iroh: connected → disconnected (3 times)"),
            report(listOf(withEvents)).lines().takeLast(2)
        )
    }

    @Test
    fun `the report never holds the password of a link`() {
        val password = RadixConnectPassword(
            value = Exactly32Bytes.init(ByteArray(32) { index -> (index * 7 + 3).toByte() }.toBagOfBytes())
        )
        val passwordBytes = ByteArray(32) { index -> (index * 7 + 3).toByte() }
        val connectionId = ConnectionIdHolder(password).id
        val linked = connector.copy(
            diagnostics = connector.diagnostics.copy(
                connectionId = connectionId,
                recentEvents = listOf(LinkEvent(atMillis = NOW, source = "link", description = "connecting over [iroh, webrtc]"))
            )
        )

        val report = report(listOf(linked))

        assertFalse(report.contains(password.value.hex, ignoreCase = true))
        assertFalse(report.contains(Base64.getEncoder().encodeToString(passwordBytes)))
        assertFalse(report.contains(connectionId))
        assertEquals(true, report.contains("(link ${connectionId.take(8)})"))
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
