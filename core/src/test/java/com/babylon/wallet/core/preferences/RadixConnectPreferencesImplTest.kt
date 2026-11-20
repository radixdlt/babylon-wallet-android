package com.babylon.wallet.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.domain.SavedIrohRelays
import rdx.works.core.preferences.RadixConnectPreferencesImpl

class RadixConnectPreferencesImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var sut: RadixConnectPreferencesImpl

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("test.preferences_pb") }
        )
        sut = RadixConnectPreferencesImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `the iroh memory of each link is kept, forgotten link by link, and cleared with the wallet`() = runBlocking {
        assertNull(sut.irohLinkMemory("link-a"))

        sut.setIrohLinkMemory("link-a", "memory a")
        sut.setIrohLinkMemory("link-b", "memory b")
        sut.setIrohLinkMemory("link-a", "memory a, newer")
        assertEquals("memory a, newer", sut.irohLinkMemory("link-a"))
        assertEquals("memory b", sut.irohLinkMemory("link-b"))

        sut.removeIrohLinkMemory("link-a")
        assertNull(sut.irohLinkMemory("link-a"))
        assertEquals("memory b", sut.irohLinkMemory("link-b"))

        sut.addIrohCapableConnectionId("link-b")
        sut.clearIrohLinkMemories()
        assertNull(sut.irohLinkMemory("link-b"))
        // Only the memories: the rest of the settings stay.
        assertEquals(setOf("link-b"), sut.irohCapableConnectionIds.first())
    }

    @Test
    fun `nothing stored means the default transport and the public relays`() = runBlocking {
        assertEquals(RadixConnectTransport.DEFAULT, sut.transport.first())
        assertEquals(SavedIrohRelays(), sut.irohRelays.first())
        assertTrue(sut.irohCapableConnectionIds.first().isEmpty())
    }

    @Test
    fun `the transport is stored`() = runBlocking {
        sut.setTransport(RadixConnectTransport.IROH)

        assertEquals(RadixConnectTransport.IROH, sut.transport.first())
    }

    @Test
    fun `the single relay of development builds becomes the current relay`() = runBlocking {
        dataStore.edit { it[RadixConnectPreferencesImpl.KEY_LEGACY_IROH_RELAY_URL] = " https://relay.example.com " }

        assertEquals(SavedIrohRelays(current = IrohRelay("https://relay.example.com")), sut.irohRelays.first())
    }

    @Test
    fun `the first update moves the single relay of development builds to the relays`() = runBlocking {
        dataStore.edit { it[RadixConnectPreferencesImpl.KEY_LEGACY_IROH_RELAY_URL] = "https://relay.example.com" }

        val updated = sut.updateIrohRelays { it.copy(others = it.others + IrohRelay("https://other.example.com")) }

        val expected = SavedIrohRelays(
            current = IrohRelay("https://relay.example.com"),
            others = listOf(IrohRelay("https://other.example.com"))
        )
        assertEquals(expected, updated)
        assertEquals(expected, sut.irohRelays.first())
        assertNull(dataStore.data.first()[RadixConnectPreferencesImpl.KEY_LEGACY_IROH_RELAY_URL])
    }

    @Test
    fun `the relay that used to be the default becomes the public relays`() = runBlocking {
        sut.updateIrohRelays {
            SavedIrohRelays(
                current = IrohRelay("https://euc1-1.relay.n0.iroh.link./"),
                others = listOf(IrohRelay("https://relay.example.com"))
            )
        }

        assertEquals(SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com"))), sut.irohRelays.first())
    }

    @Test
    fun `the relay that used to be the default is recognised without its trailing dot`() = runBlocking {
        sut.updateIrohRelays { SavedIrohRelays(current = IrohRelay("https://euc1-1.relay.n0.iroh.link/")) }

        assertEquals(SavedIrohRelays(), sut.irohRelays.first())
    }

    @Test
    fun `the single relay of development builds that was the default becomes the public relays`() = runBlocking {
        dataStore.edit { it[RadixConnectPreferencesImpl.KEY_LEGACY_IROH_RELAY_URL] = "https://euc1-1.relay.n0.iroh.link./" }

        assertEquals(SavedIrohRelays(), sut.irohRelays.first())
    }

    @Test
    fun `unreadable relays fall back to the public relays`() = runBlocking {
        dataStore.edit { it[RadixConnectPreferencesImpl.KEY_IROH_RELAYS] = "{not json" }

        assertEquals(SavedIrohRelays(), sut.irohRelays.first())
    }

    @Test
    fun `links supporting iroh are remembered and forgotten`() = runBlocking {
        sut.addIrohCapableConnectionId("a")
        sut.addIrohCapableConnectionId("b")
        sut.addIrohCapableConnectionId("a")
        assertEquals(setOf("a", "b"), sut.irohCapableConnectionIds.first())

        sut.removeIrohCapableConnectionId("a")
        assertEquals(setOf("b"), sut.irohCapableConnectionIds.first())

        sut.clearIrohCapableConnectionIds()
        assertTrue(sut.irohCapableConnectionIds.first().isEmpty())
    }
}
