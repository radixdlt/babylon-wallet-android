package rdx.works.peerdroid.data

import kotlinx.coroutines.flow.Flow
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.domain.ConnectionIdHolder

/**
 * The Radix Connect settings peerdroid follows, and what it learns about the links. Provided by the app.
 */
interface RadixConnectTransportSettings {

    /**
     * How the linked connectors are reached.
     */
    val transport: Flow<RadixConnectTransport>

    /**
     * The iroh relay server to use, or null for the library's default relay.
     */
    val irohRelayUrl: Flow<String?>

    /**
     * The ids ([ConnectionIdHolder.id]) of the links known to support iroh.
     */
    val irohCapableConnectionIds: Flow<Set<String>>

    /**
     * Remembers that the connector extension of [connectionId] supports iroh.
     */
    suspend fun rememberIrohSupport(connectionId: ConnectionIdHolder)

    /**
     * What the iroh library remembered of the link [connectionId] in its last session (opaque), or null.
     */
    suspend fun irohLinkMemory(connectionId: ConnectionIdHolder): String?

    /**
     * Keeps what the iroh library remembers of the link [connectionId], for its next session.
     */
    suspend fun rememberIrohLinkMemory(connectionId: ConnectionIdHolder, memory: String)

    /**
     * Forgets everything learned about [connectionId], once the link is deleted.
     */
    suspend fun forgetLink(connectionId: ConnectionIdHolder)
}
