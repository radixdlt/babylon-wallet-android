package rdx.works.peerdroid.domain

/**
 * The short form of a connection id used in logs and reports (it is not a secret, but the full id is noise).
 */
val ConnectionIdHolder.shortId: String
    get() = id.take(SHORT_ID_LENGTH)

private const val SHORT_ID_LENGTH = 8

/**
 * Provides connectivity snapshots of the links, for diagnostics screens and bug reports.
 */
interface RadixConnectDiagnostics {

    /**
     * A snapshot of every link currently connected (or being connected) by peerdroid.
     */
    suspend fun linkDiagnostics(): List<LinkDiagnostics>
}

/**
 * The connectivity of one link. Holds no secrets: no password, no keys, no message contents.
 *
 * @param connectionId the [ConnectionIdHolder.id] of the link.
 * @param status the merged status of its transports, null if none reports one.
 * @param transports one entry per transport the link uses.
 * @param recentEvents the last few things that happened to the link, oldest first.
 */
data class LinkDiagnostics(
    val connectionId: String,
    val status: PeerConnectionStatus?,
    val transports: List<TransportDiagnostics>,
    val recentEvents: List<LinkEvent> = emptyList()
)

/**
 * Something that happened to a link (a transport connected, a message fell back to the other transport...), for
 * the diagnostics. Holds no secrets and no message contents.
 *
 * @param atMillis when it last happened, in milliseconds since the epoch.
 * @param source the transport it happened on ("iroh", "webrtc"), or "link" for the link as a whole.
 * @param count how many times in a row it happened.
 */
data class LinkEvent(
    val atMillis: Long,
    val source: String,
    val description: String,
    val count: Int = 1
)

sealed interface TransportDiagnostics {

    /**
     * Null when the transport has no connection for the link (not started yet, or stopped).
     */
    val status: PeerConnectionStatus?

    /**
     * The last thing that went wrong, if anything.
     */
    val lastError: String?

    data class WebRtc(
        override val status: PeerConnectionStatus?,
        override val lastError: String?,
        val signalingServer: String?
    ) : TransportDiagnostics

    /**
     * @param session the snapshot of the link's iroh session, null while none runs.
     */
    data class Iroh(
        override val status: PeerConnectionStatus?,
        override val lastError: String?,
        val session: IrohSessionDiagnostics?
    ) : TransportDiagnostics
}

/**
 * A snapshot of an iroh session, as reported by the library. Holds no secrets (endpoint ids are public keys).
 *
 * @param relayUrl the relay in use: the custom one, or the public relay this end connected to (null until then).
 * @param usesPublicRelays whether this end uses n0's public relays rather than a custom one.
 * @param summary everything in one line, ready for a log.
 */
data class IrohSessionDiagnostics(
    val relayUrl: String?,
    val usesPublicRelays: Boolean,
    val isRelayConnected: Boolean,
    val relayError: String?,
    val path: IrohPath?,
    val rttMs: Long?,
    val failedAttempts: Int,
    val lastError: String?,
    val connectedForMs: Long?,
    val summary: String
)

enum class IrohPath {
    DIRECT,
    RELAY
}
