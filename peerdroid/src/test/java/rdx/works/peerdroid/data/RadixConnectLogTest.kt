package rdx.works.peerdroid.data

import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.peerdroid.domain.ConnectionIdHolder

class RadixConnectLogTest {

    @Test
    fun `a link is identified by the short form of its id only`() {
        val connectionId = ConnectionIdHolder("1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b")

        assertEquals("link=1a2b3c4d", connectionId.logId)
    }

    @Test
    fun `IP addresses are removed, with their ports`() {
        assertEquals(
            "dial failed: connection refused at <ip>, also tried <ip> and <ip>",
            "dial failed: connection refused at 192.168.1.20:443, also tried 10.0.0.1 and 2001:db8::8a2e:370:7334".redactIpAddresses()
        )
        assertEquals("addresses [<ip>]:7842 and <ip>", "addresses [fe80::1%wlan0]:7842 and 2001:0db8:85a3:0000:0000:8a2e:0370:7334".redactIpAddresses())
        assertEquals("loopback [<ip>]:443", "loopback [::1]:443".redactIpAddresses())
    }

    @Test
    fun `what is not an IP address is left alone`() {
        val lines = listOf(
            "peerdroid::signaling: connected at 12:30:45 after 3 attempts",
            "signaling=wss://signaling-server.example.com./ rtt=42ms version 1.22.2",
            "wallet 4d99dc649e → efcc89cfbf state=connected"
        )

        lines.forEach { line -> assertEquals(line, line.redactIpAddresses()) }
    }

    @Test
    fun `errors are described with their type and message, without IP addresses`() {
        assertEquals(
            "IllegalStateException could not reach <ip>",
            IllegalStateException("could not reach 203.0.113.7:3478").describe()
        )
    }
}
