package rdx.works.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import rdx.works.core.di.NonEncryptedPreferences
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.domain.SavedIrohRelays
import javax.inject.Inject

/**
 * The stored Radix Connect settings. Plain storage: the rules (validation, the built-in default relay...) live in the
 * callers.
 */
interface RadixConnectPreferences {

    val transport: Flow<RadixConnectTransport>

    val irohRelays: Flow<SavedIrohRelays>

    /**
     * The connection ids (`ConnectionIdHolder`, the hash of each link's password, which the signaling server sees
     * too) of the links known to support iroh.
     */
    val irohCapableConnectionIds: Flow<Set<String>>

    suspend fun setTransport(transport: RadixConnectTransport)

    /**
     * Atomically replaces the relays with the result of [transform] and returns it.
     */
    suspend fun updateIrohRelays(transform: (SavedIrohRelays) -> SavedIrohRelays): SavedIrohRelays

    suspend fun addIrohCapableConnectionId(connectionId: String)

    suspend fun removeIrohCapableConnectionId(connectionId: String)

    suspend fun clearIrohCapableConnectionIds()

    /**
     * What the iroh library remembers of the link [connectionId] (an opaque string with no secret: the relay the
     * connector was last seen on and when it was last connected), or null when nothing is known yet.
     */
    suspend fun irohLinkMemory(connectionId: String): String?

    suspend fun setIrohLinkMemory(connectionId: String, memory: String)

    suspend fun removeIrohLinkMemory(connectionId: String)

    /**
     * Forgets the iroh memory of every link (the wallet is deleted).
     */
    suspend fun clearIrohLinkMemories()
}

class RadixConnectPreferencesImpl @Inject constructor(
    @NonEncryptedPreferences private val dataStore: DataStore<Preferences>
) : RadixConnectPreferences {

    private val json = Json { ignoreUnknownKeys = true }

    override val transport: Flow<RadixConnectTransport> = dataStore.data
        .map { preferences -> RadixConnectTransport.fromLiteral(preferences[KEY_TRANSPORT]) }
        .distinctUntilChanged()

    override val irohRelays: Flow<SavedIrohRelays> = dataStore.data
        .map { preferences -> preferences.irohRelays() }
        .distinctUntilChanged()

    override val irohCapableConnectionIds: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[KEY_IROH_CAPABLE_CONNECTION_IDS].orEmpty() }
        .distinctUntilChanged()

    override suspend fun setTransport(transport: RadixConnectTransport) {
        dataStore.edit { preferences -> preferences[KEY_TRANSPORT] = transport.toLiteral() }
    }

    override suspend fun updateIrohRelays(transform: (SavedIrohRelays) -> SavedIrohRelays): SavedIrohRelays {
        var updated = SavedIrohRelays()
        dataStore.edit { preferences ->
            updated = transform(preferences.irohRelays())
            preferences[KEY_IROH_RELAYS] = json.encodeToString(SavedIrohRelays.serializer(), updated)
            // Migrated into the relays above.
            preferences.remove(KEY_LEGACY_IROH_RELAY_URL)
        }
        return updated
    }

    override suspend fun addIrohCapableConnectionId(connectionId: String) {
        dataStore.edit { preferences -> preferences.updateIrohCapableConnectionIds { it + connectionId } }
    }

    override suspend fun removeIrohCapableConnectionId(connectionId: String) {
        dataStore.edit { preferences -> preferences.updateIrohCapableConnectionIds { it - connectionId } }
    }

    override suspend fun clearIrohCapableConnectionIds() {
        dataStore.edit { preferences -> preferences.remove(KEY_IROH_CAPABLE_CONNECTION_IDS) }
    }

    override suspend fun irohLinkMemory(connectionId: String): String? =
        dataStore.data.first()[irohLinkMemoryKey(connectionId)]

    override suspend fun setIrohLinkMemory(connectionId: String, memory: String) {
        dataStore.edit { preferences -> preferences[irohLinkMemoryKey(connectionId)] = memory }
    }

    override suspend fun removeIrohLinkMemory(connectionId: String) {
        dataStore.edit { preferences -> preferences.remove(irohLinkMemoryKey(connectionId)) }
    }

    override suspend fun clearIrohLinkMemories() {
        dataStore.edit { preferences ->
            preferences.asMap().keys
                .filter { key -> key.name.startsWith(IROH_LINK_MEMORY_KEY_PREFIX) }
                .forEach { key -> preferences.remove(key) }
        }
    }

    private fun irohLinkMemoryKey(connectionId: String) = stringPreferencesKey(IROH_LINK_MEMORY_KEY_PREFIX + connectionId)

    // Development builds stored a single custom relay URL: it becomes the current relay. The single relay that used to
    // be the built-in default became n0's public relays (no current relay).
    private fun Preferences.irohRelays(): SavedIrohRelays {
        val stored = this[KEY_IROH_RELAYS] ?: return SavedIrohRelays(
            current = this[KEY_LEGACY_IROH_RELAY_URL]?.trim()?.takeIf { it.isNotEmpty() }?.let { IrohRelay(url = it) }
        ).withoutLegacyDefaultRelay()
        // Unreadable (corrupted) relays fall back to the public relays rather than break Radix Connect.
        return runCatching { json.decodeFromString(SavedIrohRelays.serializer(), stored) }
            .getOrElse { SavedIrohRelays() }
            .withoutLegacyDefaultRelay()
    }

    private fun SavedIrohRelays.withoutLegacyDefaultRelay(): SavedIrohRelays =
        if (current != null && current.url.normalizedRelayUrl() == LEGACY_DEFAULT_RELAY_URL.normalizedRelayUrl()) {
            copy(current = null)
        } else {
            this
        }

    // The legacy default was written with and without its trailing "./".
    private fun String.normalizedRelayUrl(): String = trim().trimEnd('/').trimEnd('.').lowercase()

    private fun MutablePreferences.updateIrohCapableConnectionIds(transform: (Set<String>) -> Set<String>) {
        val updated = transform(this[KEY_IROH_CAPABLE_CONNECTION_IDS].orEmpty())
        if (updated.isEmpty()) {
            remove(KEY_IROH_CAPABLE_CONNECTION_IDS)
        } else {
            this[KEY_IROH_CAPABLE_CONNECTION_IDS] = updated
        }
    }

    companion object {
        val KEY_TRANSPORT = stringPreferencesKey("radix_connect_transport")
        val KEY_IROH_RELAYS = stringPreferencesKey("radix_connect_iroh_relays")
        val KEY_IROH_CAPABLE_CONNECTION_IDS = stringSetPreferencesKey("radix_connect_iroh_capable_connection_ids")

        // Followed by the link's connection id (the hash of its password, never the password).
        const val IROH_LINK_MEMORY_KEY_PREFIX = "radix_connect_iroh_link_memory_"

        // The single relay URL stored by development builds.
        val KEY_LEGACY_IROH_RELAY_URL = stringPreferencesKey("iroh_relay_url")

        // The single relay used by default before n0's public relays (with address lookup) became the default.
        const val LEGACY_DEFAULT_RELAY_URL = "https://euc1-1.relay.n0.iroh.link./"
    }
}
