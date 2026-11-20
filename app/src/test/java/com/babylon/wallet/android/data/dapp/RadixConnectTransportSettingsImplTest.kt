package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.fakes.FakeRadixConnectPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.domain.SavedIrohRelays
import rdx.works.peerdroid.domain.ConnectionIdHolder

class RadixConnectTransportSettingsImplTest {

    private val preferences = FakeRadixConnectPreferences()
    private val sut = RadixConnectTransportSettingsImpl(preferences)

    @Test
    fun `by default the default transport and the default relay are used`() = runTest {
        assertEquals(RadixConnectTransport.DEFAULT, sut.transport.first())
        assertNull(sut.irohRelayUrl.first())
    }

    @Test
    fun `the selected transport is used, in every build`() = runTest {
        preferences.setTransport(RadixConnectTransport.IROH)

        assertEquals(RadixConnectTransport.IROH, sut.transport.first())
    }

    @Test
    fun `the current relay server is used`() = runTest {
        preferences.irohRelaysState.value = SavedIrohRelays(
            current = IrohRelay("https://relay.example.com"),
            others = listOf(IrohRelay("https://other.example.com"))
        )

        assertEquals("https://relay.example.com", sut.irohRelayUrl.first())
    }

    @Test
    fun `iroh support is remembered and forgotten per link`() = runTest {
        val connectionId = ConnectionIdHolder(id = "abcdef0123456789")

        sut.rememberIrohSupport(connectionId)
        assertEquals(setOf("abcdef0123456789"), sut.irohCapableConnectionIds.first())

        sut.forgetLink(connectionId)
        assertTrue(sut.irohCapableConnectionIds.first().isEmpty())
    }
}
