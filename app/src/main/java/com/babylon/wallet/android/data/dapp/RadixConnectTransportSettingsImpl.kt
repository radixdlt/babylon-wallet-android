package com.babylon.wallet.android.data.dapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.preferences.RadixConnectPreferences
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.domain.ConnectionIdHolder
import javax.inject.Inject

/**
 * The Radix Connect settings of Settings → Preferences → Radix Connect, and what peerdroid learns about the links.
 */
class RadixConnectTransportSettingsImpl @Inject constructor(
    private val radixConnectPreferences: RadixConnectPreferences
) : RadixConnectTransportSettings {

    override val transport: Flow<RadixConnectTransport> = radixConnectPreferences.transport

    override val irohRelayUrl: Flow<String?> = radixConnectPreferences.irohRelays
        .map { relays -> relays.current?.url }
        .distinctUntilChanged()

    override val irohCapableConnectionIds: Flow<Set<String>> = radixConnectPreferences.irohCapableConnectionIds

    override suspend fun rememberIrohSupport(connectionId: ConnectionIdHolder) {
        radixConnectPreferences.addIrohCapableConnectionId(connectionId.id)
    }

    override suspend fun irohLinkMemory(connectionId: ConnectionIdHolder): String? =
        radixConnectPreferences.irohLinkMemory(connectionId.id)

    override suspend fun rememberIrohLinkMemory(connectionId: ConnectionIdHolder, memory: String) {
        radixConnectPreferences.setIrohLinkMemory(connectionId.id, memory)
    }

    override suspend fun forgetLink(connectionId: ConnectionIdHolder) {
        radixConnectPreferences.removeIrohCapableConnectionId(connectionId.id)
        radixConnectPreferences.removeIrohLinkMemory(connectionId.id)
    }
}
