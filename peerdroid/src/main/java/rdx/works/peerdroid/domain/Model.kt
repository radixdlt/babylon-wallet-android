package rdx.works.peerdroid.domain

import kotlinx.coroutines.Job
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient

@JvmInline
value class ConnectionIdHolder(
    val id: String
)

@JvmInline
value class RemoteClientHolder(
    val id: String
)

internal data class WebSocketHolder(
    val webSocketClient: WebSocketClient,
    val listenMessagesJob: Job
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
