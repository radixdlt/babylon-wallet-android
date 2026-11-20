package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.domain.TransportDiagnostics
import java.net.URI

@StringRes
fun RadixConnectTransport.titleRes(): Int = when (this) {
    RadixConnectTransport.AUTOMATIC -> R.string.preferences_radixConnect_automatic_title
    RadixConnectTransport.WEBRTC -> R.string.preferences_radixConnect_webRtc_title
    RadixConnectTransport.IROH -> R.string.preferences_radixConnect_iroh_title
}

@StringRes
fun RadixConnectTransport.subtitleRes(): Int = when (this) {
    RadixConnectTransport.AUTOMATIC -> R.string.preferences_radixConnect_automatic_subtitle
    RadixConnectTransport.WEBRTC -> R.string.preferences_radixConnect_webRtc_subtitle
    RadixConnectTransport.IROH -> R.string.preferences_radixConnect_iroh_subtitle
}

/**
 * The hosts of relay URLs, e.g. "euc1-1.relay.n0.iroh.link, use1-1.relay.n0.iroh.link", for display.
 */
fun List<String>.relayHosts(): String = joinToString(separator = ", ") { url ->
    runCatching { URI(url.trim()).host }.getOrNull()?.trimEnd('.') ?: url
}

/**
 * A URL as shown to people: iroh gives relay URLs with the absolute DNS name, e.g.
 * "https://euc1-1.relay.n0.iroh.link./", whose final dot reads as a typo; it is dropped.
 */
fun String.withoutTrailingHostDot(): String = replace(TRAILING_HOST_DOT, "\$1")

private val TRAILING_HOST_DOT = Regex("""(://[^/?#:]+)\.(?=[/?#:]|$)""")

/** Which transport's diagnostics a screen shows. */
enum class DiagnosedTransport {
    IROH,
    WEBRTC;

    fun shows(transport: TransportDiagnostics): Boolean = when (this) {
        IROH -> transport is TransportDiagnostics.Iroh
        WEBRTC -> transport is TransportDiagnostics.WebRtc
    }
}
