package rdx.works.peerdroid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.data.PeerdroidTransport.IROH
import rdx.works.peerdroid.data.PeerdroidTransport.WEBRTC
import rdx.works.peerdroid.data.fakes.password
import rdx.works.peerdroid.domain.ConnectionIdHolder

class TransportPolicyTest {

    private val irohLink = ConnectionIdHolder(password(seed = 1))
    private val webRtcLink = ConnectionIdHolder(password(seed = 2))
    private val irohCapableConnectionIds = setOf(irohLink.id)

    private fun policy(mode: RadixConnectTransport, irohCapable: Set<String> = irohCapableConnectionIds) =
        TransportPolicy(mode = mode, irohCapableConnectionIds = irohCapable)

    @Test
    fun `the transports of each link per mode and capability`() {
        // mode, link supports iroh, transports
        val matrix = listOf(
            Triple(RadixConnectTransport.AUTOMATIC, true, setOf(IROH, WEBRTC)),
            Triple(RadixConnectTransport.AUTOMATIC, false, setOf(WEBRTC)),
            Triple(RadixConnectTransport.WEBRTC, true, setOf(WEBRTC)),
            Triple(RadixConnectTransport.WEBRTC, false, setOf(WEBRTC)),
            Triple(RadixConnectTransport.IROH, true, setOf(IROH)),
            Triple(RadixConnectTransport.IROH, false, setOf(IROH))
        )

        matrix.forEach { (mode, supportsIroh, transports) ->
            val link = if (supportsIroh) irohLink else webRtcLink
            assertEquals("$mode, iroh: $supportsIroh", transports, policy(mode).transportsFor(link))
        }
    }

    @Test
    fun `linking tries the transports of the mode`() {
        assertEquals(setOf(IROH, WEBRTC), policy(RadixConnectTransport.AUTOMATIC).linkingTransports())
        assertEquals(setOf(WEBRTC), policy(RadixConnectTransport.WEBRTC).linkingTransports())
        assertEquals(setOf(IROH), policy(RadixConnectTransport.IROH).linkingTransports())
    }

    @Test
    fun `in automatic mode iroh is used only once some link supports it`() {
        assertTrue(policy(RadixConnectTransport.AUTOMATIC).mayUse(IROH))
        assertFalse(policy(RadixConnectTransport.AUTOMATIC, irohCapable = emptySet()).mayUse(IROH))
        assertTrue(policy(RadixConnectTransport.AUTOMATIC, irohCapable = emptySet()).mayUse(WEBRTC))
    }

    @Test
    fun `single transport modes use only their transport`() {
        assertTrue(policy(RadixConnectTransport.WEBRTC).mayUse(WEBRTC))
        assertFalse(policy(RadixConnectTransport.WEBRTC).mayUse(IROH))
        assertTrue(policy(RadixConnectTransport.IROH).mayUse(IROH))
        assertFalse(policy(RadixConnectTransport.IROH).mayUse(WEBRTC))
    }

    @Test
    fun `every transport has a rule in every mode`() {
        RadixConnectTransport.entries.forEach { mode ->
            val transports = policy(mode).transportsFor(irohLink) + policy(mode).linkingTransports()
            assertTrue("$mode uses no transport", transports.isNotEmpty())
        }
    }
}
