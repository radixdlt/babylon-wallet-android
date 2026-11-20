package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.IrohPath
import rdx.works.peerdroid.domain.IrohSessionDiagnostics
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.LinkEvent
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics
import rdx.works.peerdroid.domain.shortId
import java.util.Locale

/**
 * The diagnostics of one linked connector, with its name.
 */
data class ConnectorDiagnostics(
    val name: String,
    val diagnostics: LinkDiagnostics
) {
    val shortId: String
        get() = ConnectionIdHolder(diagnostics.connectionId).shortId
}

/**
 * A plain text report of the Radix Connect diagnostics, for bug reports. It holds no secrets: no passwords or keys,
 * no message contents, only the short form of the connection ids.
 *
 * Per connector: its merged status, then each transport (status, relay, path, RTT, failed attempts, connected for,
 * last error) and the last few things that happened to it.
 */
object RadixConnectDiagnosticsReport {

    /**
     * @param nowMillis the current time, in milliseconds since the epoch, for the age of the recent events.
     */
    fun build(
        appVersion: String,
        transport: RadixConnectTransport,
        relayServer: IrohRelayServer?,
        connectors: List<ConnectorDiagnostics>,
        nowMillis: Long = System.currentTimeMillis()
    ): String = buildString {
        appendLine("Radix Connect diagnostics")
        appendLine("App version: $appVersion")
        appendLine("Connection method: ${transport.name.lowercase(Locale.ROOT)}")
        appendLine("iroh relay server: ${relayServer.describe()}")
        appendLine("Connectors: ${connectors.size}")
        connectors.forEach { connector ->
            appendLine()
            appendLine("\"${connector.name}\" (link ${connector.shortId}): ${connector.diagnostics.status.describe()}")
            if (connector.diagnostics.transports.isEmpty()) {
                appendLine("  no transport")
            }
            connector.diagnostics.transports.forEach { transportDiagnostics ->
                appendLine("  ${transportDiagnostics.describe()}")
            }
            val recentEvents = connector.diagnostics.recentEvents.takeLast(MAX_REPORTED_EVENTS)
            if (recentEvents.isNotEmpty()) {
                appendLine("  recent events:")
                recentEvents.forEach { event -> appendLine("    ${event.describe(nowMillis)}") }
            }
        }
    }.trimEnd()

    private fun LinkEvent.describe(nowMillis: Long): String {
        val repetitions = if (count > 1) " ($count times)" else ""
        return "${age(nowMillis - atMillis)}: $source: $description$repetitions"
    }

    private fun age(elapsedMs: Long): String {
        val seconds = elapsedMs.coerceAtLeast(0) / MILLIS_PER_SECOND
        return when {
            seconds < SECONDS_PER_MINUTE -> "$seconds s ago"
            seconds < SECONDS_PER_HOUR -> "${seconds / SECONDS_PER_MINUTE} min ago"
            else -> "${seconds / SECONDS_PER_HOUR} h ago"
        }
    }

    fun TransportDiagnostics.describe(): String = when (this) {
        is TransportDiagnostics.WebRtc -> listOf(
            "WebRTC link ${status.describe()}",
            "signaling server ${signalingServer ?: "-"}",
            "last error: ${lastError ?: "none"}"
        ).joinToString("; ")

        // The link to the connector first; the relay is this wallet's own and can be reachable while the link is
        // closed, so it is said so apart ("reachable"), not "connected" next to the link's state.
        is TransportDiagnostics.Iroh -> listOfNotNull(
            "iroh link ${status.describe()}",
            session?.let {
                "this wallet's relay ${it.relayDescription()} (${if (it.isRelayConnected) "reachable" else "NOT reachable"})"
            },
            session?.relayError?.let { "relay error: $it" },
            session?.path?.let { "path ${it.describe()}" },
            session?.rttMs?.let { "RTT $it ms" },
            session?.let { "failed attempts ${it.failedAttempts}" },
            session?.connectedForMs?.let { "connected for ${it / MILLIS_PER_SECOND} s" },
            "last error: ${lastError ?: "none"}"
        ).joinToString("; ")
    }

    private fun IrohPath.describe() = when (this) {
        IrohPath.DIRECT -> "direct"
        IrohPath.RELAY -> "relay"
    }

    private fun PeerConnectionStatus?.describe() = this?.name?.lowercase(Locale.ROOT) ?: "not running"

    private fun IrohRelayServer?.describe() = when (this) {
        is IrohRelayServer.Custom -> url
        IrohRelayServer.Public, null -> "public (closest, n0)"
    }

    // The public relay this end connected to, or the custom one.
    private fun IrohSessionDiagnostics.relayDescription() = if (usesPublicRelays) {
        "public, ${relayUrl?.withoutTrailingHostDot() ?: "not chosen yet"}"
    } else {
        relayUrl?.withoutTrailingHostDot() ?: "-"
    }

    private const val MILLIS_PER_SECOND = 1_000
    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3_600
    private const val MAX_REPORTED_EVENTS = 8
}
