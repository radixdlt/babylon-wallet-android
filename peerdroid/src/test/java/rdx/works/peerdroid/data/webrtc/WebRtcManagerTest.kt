package rdx.works.peerdroid.data.webrtc

import com.radixdlt.sargon.P2pStunServer
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.P2pTurnServer
import org.junit.Test
import kotlin.test.assertEquals

class WebRtcManagerTest {

    @Test
    fun givenTransportProfileWithoutTurnUrls_whenBuildingIceServers_thenOnlyStunServerIsConfigured() {
        val transportProfile = P2pTransportProfile(
            name = "Test",
            signalingServer = "wss://signal.example.com",
            stun = P2pStunServer(
                urls = listOf("stun:stun.example.com:3478")
            ),
            turn = P2pTurnServer(
                username = null,
                credential = null,
                urls = emptyList()
            )
        )

        val iceServers = createIceServers(transportProfile)

        assertEquals(1, iceServers.size)
        assertEquals(listOf("stun:stun.example.com:3478"), iceServers.single().urls)
    }
}
