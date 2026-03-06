package rdx.works.peerdroid.domain

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import kotlinx.coroutines.Job
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient

@JvmInline
value class ConnectionIdHolder(
    val id: String
) {

    constructor(password: RadixConnectPassword) : this(password.value.bytes.hash().hex)

    constructor(p2pLink: P2pLink) : this(p2pLink.clientID().hex)
}

@JvmInline
value class RemoteClientHolder(
    val id: String
)

internal data class WebSocketHolder(
    val webSocketClient: WebSocketClient,
    val listenMessagesJob: Job,
    val p2pTransportProfile: P2pTransportProfile
)

internal data class PeerConnectionHolder(
    val connectionIdHolder: ConnectionIdHolder,
    val webRtcManager: WebRtcManager,
    val observePeerConnectionJob: Job
)

internal data class DataChannelHolder(
    val connectionIdHolder: ConnectionIdHolder,
    val dataChannel: DataChannelWrapper
)
