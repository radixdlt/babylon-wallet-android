package rdx.works.core.domain

private const val LITERAL_AUTOMATIC = "automatic"
private const val LITERAL_WEBRTC = "webrtc"
private const val LITERAL_IROH = "iroh"

/**
 * How Radix Connect reaches the linked connectors.
 */
enum class RadixConnectTransport(
    private val literal: String
) {
    /**
     * WebRTC for every link, plus iroh for the links known to support it. Linking tries both and uses whichever
     * connects first.
     */
    AUTOMATIC(literal = LITERAL_AUTOMATIC),

    /**
     * WebRTC data channels negotiated through the signaling server, for every link.
     */
    WEBRTC(literal = LITERAL_WEBRTC),

    /**
     * QUIC connections through an iroh relay server, for every link. Connectors without iroh do not connect.
     */
    IROH(literal = LITERAL_IROH);

    internal fun toLiteral() = literal

    companion object {
        /**
         * iroh is opt-in: until the user picks [AUTOMATIC] or [IROH], the wallet only uses WebRTC and never
         * contacts an iroh relay.
         */
        val DEFAULT = WEBRTC

        /**
         * A missing or unknown value (e.g. one stored by a newer version of the app) falls back to [DEFAULT].
         */
        internal fun fromLiteral(literal: String?): RadixConnectTransport =
            entries.firstOrNull { it.literal == literal } ?: DEFAULT
    }
}
