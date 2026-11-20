package com.babylon.wallet.android.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.domain.SavedIrohRelays
import rdx.works.core.preferences.RadixConnectPreferences

/**
 * In-memory [RadixConnectPreferences].
 */
class FakeRadixConnectPreferences(
    transport: RadixConnectTransport = RadixConnectTransport.DEFAULT,
    irohRelays: SavedIrohRelays = SavedIrohRelays(),
    irohCapableConnectionIds: Set<String> = emptySet()
) : RadixConnectPreferences {

    val transportState = MutableStateFlow(transport)
    val irohRelaysState = MutableStateFlow(irohRelays)
    val irohCapableConnectionIdsState = MutableStateFlow(irohCapableConnectionIds)

    override val transport: Flow<RadixConnectTransport> = transportState
    override val irohRelays: Flow<SavedIrohRelays> = irohRelaysState
    override val irohCapableConnectionIds: Flow<Set<String>> = irohCapableConnectionIdsState

    override suspend fun setTransport(transport: RadixConnectTransport) {
        transportState.value = transport
    }

    override suspend fun updateIrohRelays(transform: (SavedIrohRelays) -> SavedIrohRelays): SavedIrohRelays {
        // Like the DataStore: nothing is stored when the transform throws.
        val updated = transform(irohRelaysState.value)
        irohRelaysState.value = updated
        return updated
    }

    override suspend fun addIrohCapableConnectionId(connectionId: String) {
        irohCapableConnectionIdsState.value = irohCapableConnectionIdsState.value + connectionId
    }

    override suspend fun removeIrohCapableConnectionId(connectionId: String) {
        irohCapableConnectionIdsState.value = irohCapableConnectionIdsState.value - connectionId
    }

    override suspend fun clearIrohCapableConnectionIds() {
        irohCapableConnectionIdsState.value = emptySet()
    }

    val irohLinkMemories = mutableMapOf<String, String>()

    override suspend fun irohLinkMemory(connectionId: String): String? = irohLinkMemories[connectionId]

    override suspend fun setIrohLinkMemory(connectionId: String, memory: String) {
        irohLinkMemories[connectionId] = memory
    }

    override suspend fun removeIrohLinkMemory(connectionId: String) {
        irohLinkMemories.remove(connectionId)
    }

    override suspend fun clearIrohLinkMemories() {
        irohLinkMemories.clear()
    }
}
