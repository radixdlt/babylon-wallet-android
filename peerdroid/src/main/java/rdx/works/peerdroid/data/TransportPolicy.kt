package rdx.works.peerdroid.data

import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.domain.ConnectionIdHolder

/**
 * The transports peerdroid can reach a connector extension with. The declaration order is the order of preference
 * for sending a message when several are connected.
 */
internal enum class PeerdroidTransport {
    IROH,
    WEBRTC
}

/**
 * Decides which transports each link uses. All the per-transport rules live here: the routing only asks the policy,
 * so a new transport needs a [PeerdroidTransport] entry, its connector and a rule below.
 *
 * @param mode the selected [RadixConnectTransport].
 * @param irohCapableConnectionIds the ids ([ConnectionIdHolder.id]) of the links known to support iroh.
 */
internal data class TransportPolicy(
    val mode: RadixConnectTransport,
    val irohCapableConnectionIds: Set<String>
) {

    /**
     * The transports that connect to the connector extension of [connectionId].
     */
    fun transportsFor(connectionId: ConnectionIdHolder): Set<PeerdroidTransport> = when (mode) {
        // WebRTC keeps every existing connector working; iroh only where it is known to work.
        RadixConnectTransport.AUTOMATIC -> if (connectionId.id in irohCapableConnectionIds) {
            setOf(PeerdroidTransport.IROH, PeerdroidTransport.WEBRTC)
        } else {
            setOf(PeerdroidTransport.WEBRTC)
        }

        RadixConnectTransport.WEBRTC -> setOf(PeerdroidTransport.WEBRTC)
        RadixConnectTransport.IROH -> setOf(PeerdroidTransport.IROH)
    }

    /**
     * The transports that try to link a new connector extension (nothing is known about it yet): the first one that
     * connects completes the linking.
     */
    fun linkingTransports(): Set<PeerdroidTransport> = when (mode) {
        RadixConnectTransport.AUTOMATIC -> setOf(PeerdroidTransport.IROH, PeerdroidTransport.WEBRTC)
        RadixConnectTransport.WEBRTC -> setOf(PeerdroidTransport.WEBRTC)
        RadixConnectTransport.IROH -> setOf(PeerdroidTransport.IROH)
    }

    /**
     * Whether [transport] may connect any link at all.
     */
    fun mayUse(transport: PeerdroidTransport): Boolean = when (mode) {
        RadixConnectTransport.AUTOMATIC -> transport == PeerdroidTransport.WEBRTC || irohCapableConnectionIds.isNotEmpty()
        RadixConnectTransport.WEBRTC -> transport == PeerdroidTransport.WEBRTC
        RadixConnectTransport.IROH -> transport == PeerdroidTransport.IROH
    }
}
