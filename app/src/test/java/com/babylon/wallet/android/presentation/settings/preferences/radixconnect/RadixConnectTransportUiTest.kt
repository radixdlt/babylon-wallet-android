package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import org.junit.Assert.assertEquals
import org.junit.Test

class RadixConnectTransportUiTest {

    @Test
    fun `the final dot of an absolute host name is not shown`() {
        assertEquals("https://euc1-1.relay.n0.iroh.link/", "https://euc1-1.relay.n0.iroh.link./".withoutTrailingHostDot())
        assertEquals("https://relay.example.com:443/x.y", "https://relay.example.com.:443/x.y".withoutTrailingHostDot())
        assertEquals("https://relay.example.com", "https://relay.example.com.".withoutTrailingHostDot())
    }

    @Test
    fun `a URL without that dot is shown as it is`() {
        assertEquals("https://relay.example.com/a.", "https://relay.example.com/a.".withoutTrailingHostDot())
    }

    @Test
    fun `relay hosts are listed without the final dot`() {
        assertEquals(
            "euc1-1.relay.n0.iroh.link, use1-1.relay.n0.iroh.link",
            listOf("https://euc1-1.relay.n0.iroh.link./", "https://use1-1.relay.n0.iroh.link./").relayHosts()
        )
    }
}
