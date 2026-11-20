package com.babylon.wallet.android.data.repository.radixconnect

import com.babylon.wallet.android.fakes.FakeIrohRelayConfig
import com.babylon.wallet.android.fakes.FakeRadixConnectPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.SavedIrohRelays

@OptIn(ExperimentalCoroutinesApi::class)
class IrohRelaysRepositoryTest {

    private val preferences = FakeRadixConnectPreferences()
    private val relayConfig = FakeIrohRelayConfig()
    private val sut = IrohRelaysRepositoryImpl(
        radixConnectPreferences = preferences,
        irohRelayConfig = relayConfig,
        defaultDispatcher = UnconfinedTestDispatcher()
    )

    private val relayA = IrohRelayServer.Custom("https://a.example.com")
    private val relayB = IrohRelayServer.Custom("https://b.example.com")

    @Test
    fun `the public relays are current when nothing was chosen`() = runTest {
        assertEquals(IrohRelayServers(current = IrohRelayServer.Public, others = emptyList()), sut.relayServers.first())
    }

    @Test
    fun `added relay servers are listed after the current one`() = runTest {
        sut.add(" https://a.example.com ")
        sut.add("https://b.example.com")

        assertEquals(
            IrohRelayServers(current = IrohRelayServer.Public, others = listOf(relayA, relayB)),
            sut.relayServers.first()
        )
    }

    @Test
    fun `invalid relay server URLs are rejected with the reason`() = runTest {
        val result = sut.add("wss://a.example.com")

        val error = result.exceptionOrNull() as IrohRelayServerException.InvalidUrl
        assertEquals("the scheme must be https or http", error.reason)
        assertEquals(SavedIrohRelays(), preferences.irohRelaysState.value)
    }

    @Test
    fun `an empty URL is rejected`() = runTest {
        assertTrue(sut.validate("  ").exceptionOrNull() is IrohRelayServerException.InvalidUrl)
    }

    @Test
    fun `a relay server can be added only once`() = runTest {
        sut.add("https://a.example.com")

        assertTrue(sut.add("https://a.example.com").exceptionOrNull() is IrohRelayServerException.AlreadyAdded)
    }

    @Test
    fun `making a relay server current moves the previous one to the others`() = runTest {
        sut.add("https://a.example.com")
        sut.add("https://b.example.com")

        sut.makeCurrent(relayB)
        assertEquals(
            IrohRelayServers(current = relayB, others = listOf(IrohRelayServer.Public, relayA)),
            sut.relayServers.first()
        )

        sut.makeCurrent(relayA)
        assertEquals(
            IrohRelayServers(current = relayA, others = listOf(IrohRelayServer.Public, relayB)),
            sut.relayServers.first()
        )

        sut.makeCurrent(IrohRelayServer.Public)
        assertEquals(
            IrohRelayServers(current = IrohRelayServer.Public, others = listOf(relayA, relayB)),
            sut.relayServers.first()
        )
    }

    @Test
    fun `an unknown relay server cannot be made current`() = runTest {
        assertTrue(sut.makeCurrent(relayA).exceptionOrNull() is IrohRelayServerException.NotFound)
    }

    @Test
    fun `the public relays cannot be removed`() = runTest {
        val result = sut.remove(IrohRelayServer.Public)

        assertTrue(result.exceptionOrNull() is IrohRelayServerException.PublicCannotBeRemoved)
    }

    @Test
    fun `removing the current relay server makes the public relays current`() = runTest {
        sut.add("https://a.example.com")
        sut.add("https://b.example.com")
        sut.makeCurrent(relayA)

        sut.remove(relayA)

        assertEquals(
            IrohRelayServers(current = IrohRelayServer.Public, others = listOf(relayB)),
            sut.relayServers.first()
        )
    }

    @Test
    fun `editing a relay server keeps its place`() = runTest {
        sut.add("https://a.example.com")
        sut.add("https://b.example.com")
        sut.makeCurrent(relayA)

        sut.update(relayA, "https://c.example.com")
        sut.update(relayB, "https://d.example.com")

        assertEquals(
            SavedIrohRelays(current = IrohRelay("https://c.example.com"), others = listOf(IrohRelay("https://d.example.com"))),
            preferences.irohRelaysState.value
        )
    }

    @Test
    fun `editing a relay server into another one already listed is rejected`() = runTest {
        sut.add("https://a.example.com")
        sut.add("https://b.example.com")

        assertTrue(sut.update(relayA, "https://b.example.com").exceptionOrNull() is IrohRelayServerException.AlreadyAdded)
    }

    @Test
    fun `relay servers cannot be validated without the iroh library`() = runTest {
        relayConfig.isLibraryAvailable = false

        assertTrue(sut.add("https://a.example.com").isFailure)
        assertEquals(SavedIrohRelays(), preferences.irohRelaysState.value)
    }
}
