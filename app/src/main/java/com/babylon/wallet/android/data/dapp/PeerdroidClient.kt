package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.dapp.model.walletRequestJson
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    suspend fun connectToRemotePeerWithEncryptionKey(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendMessage(message: String): Result<Unit>

    fun listenForStateEvents(): Flow<MessageFromDataChannel.ConnectionStateChanged>

    fun listenForIncomingRequests(): Flow<MessageFromDataChannel>

    suspend fun close(
        shouldCloseConnectionToSignalingServer: Boolean = false,
        isDeleteConnectionEvent: Boolean = false
    )

    val isAlreadyOpen: Boolean
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector
) : PeerdroidClient {

    private var dataChannel: DataChannelWrapper? = null

    override val isAlreadyOpen: Boolean
        get() = dataChannel?.state == DataChannelEvent.StateChanged.OPEN

    override suspend fun connectToRemotePeerWithEncryptionKey(
        encryptionKey: ByteArray,
    ): Result<Unit> {
        val result = peerdroidConnector.createDataChannel(
            encryptionKey = encryptionKey
        )
        return when (result) {
            is Result.Success -> {
                dataChannel = result.data
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.d("data channel failed to initialize")
                Result.Error("data channel failed to initialize")
            }
        }
    }

    override suspend fun sendMessage(message: String): Result<Unit> {
        return dataChannel
            ?.sendMessage(message)
            ?: Result.Error("data channel is null")
    }

    override fun listenForStateEvents(): Flow<MessageFromDataChannel.ConnectionStateChanged> {
        return dataChannel
            ?.dataChannelEvents
            ?.cancellable()
            ?.filterIsInstance<DataChannelEvent.StateChanged>()
            ?.map { stateChanged ->
                parseDataChannelState(stateChanged)
            }
            ?: emptyFlow()
    }

    override fun listenForIncomingRequests(): Flow<MessageFromDataChannel> {
        return dataChannel
            ?.dataChannelEvents
            ?.cancellable()
            ?.map { dataChannelEvent ->
                when (dataChannelEvent) {
                    is DataChannelEvent.StateChanged -> {
                        parseDataChannelState(dataChannelEvent)
                    }
                    is DataChannelEvent.IncomingMessage.DecodedMessage -> {
                        parseIncomingMessage(messageInJsonString = dataChannelEvent.message)
                    }
                    else -> { // TODO later we might need to handle other cases here
                        MessageFromDataChannel.None
                    }
                }
            }?.catch { exception ->
                Timber.e("caught exception: ${exception.localizedMessage}")
                if (exception is SerializationException) {
                    emit(MessageFromDataChannel.ParsingError)
                } else {
                    throw exception
                }
            }
            ?: emptyFlow()
    }

    override suspend fun close(
        shouldCloseConnectionToSignalingServer: Boolean,
        isDeleteConnectionEvent: Boolean
    ) {
        dataChannel?.close(isDeleteConnectionEvent = isDeleteConnectionEvent)
        dataChannel = null
        peerdroidConnector.close(shouldCloseConnectionToSignalingServer)
    }

    private fun parseDataChannelState(
        dataChannelEvent: DataChannelEvent.StateChanged
    ): MessageFromDataChannel.ConnectionStateChanged {
        return when (dataChannelEvent) {
            DataChannelEvent.StateChanged.CONNECTING -> {
                MessageFromDataChannel.ConnectionStateChanged.CONNECTING
            }
            DataChannelEvent.StateChanged.OPEN -> {
                MessageFromDataChannel.ConnectionStateChanged.OPEN
            }
            DataChannelEvent.StateChanged.CLOSING -> {
                MessageFromDataChannel.ConnectionStateChanged.CLOSING
            }
            DataChannelEvent.StateChanged.CLOSE -> {
                MessageFromDataChannel.ConnectionStateChanged.CLOSE
            }
            DataChannelEvent.StateChanged.DELETE_CONNECTION -> {
                MessageFromDataChannel.ConnectionStateChanged.DELETE_CONNECTION
            }
            DataChannelEvent.StateChanged.UNKNOWN -> {
                MessageFromDataChannel.ConnectionStateChanged.ERROR
            }
        }
    }

    private fun parseIncomingMessage(messageInJsonString: String): MessageFromDataChannel.IncomingRequest {
        val request = walletRequestJson.decodeFromString<WalletInteraction>(messageInJsonString)
        return request.toDomainModel()
    }
}
