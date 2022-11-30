package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    suspend fun connectToRemotePeerWithEncryptionKey(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendMessage(message: String): Result<Unit>

    fun listenForEvents(): Flow<ConnectionState>

    suspend fun close()

    val isAlreadyOpen: Boolean
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector
) : PeerdroidClient {

    private var dataChannel: DataChannelWrapper? = null

    override val isAlreadyOpen: Boolean
        get() = dataChannel?.state == DataChannelEvent.StateChanged.OPEN

    override suspend fun connectToRemotePeerWithEncryptionKey(
        encryptionKey: ByteArray
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

    override fun listenForEvents(): Flow<ConnectionState> {
        return dataChannel
            ?.dataChannelEvents
            ?.onStart {
                // we don't know when the collection will start
                // so check if the data channel is already closed
                if (dataChannel?.state == DataChannelEvent.StateChanged.CLOSING ||
                    dataChannel?.state == DataChannelEvent.StateChanged.CLOSE
                ) {
                    emit(DataChannelEvent.StateChanged.CLOSE)
                }
            }
            ?.filterIsInstance<DataChannelEvent.StateChanged>()
            ?.map { stateChanged ->
                when (stateChanged) {
                    DataChannelEvent.StateChanged.CONNECTING -> {
                        ConnectionState.CONNECTING
                    }
                    DataChannelEvent.StateChanged.OPEN -> {
                        ConnectionState.OPEN
                    }
                    DataChannelEvent.StateChanged.CLOSING -> {
                        ConnectionState.CLOSING
                    }
                    DataChannelEvent.StateChanged.CLOSE -> {
                        ConnectionState.CLOSE
                    }
                    DataChannelEvent.StateChanged.UNKNOWN -> {
                        ConnectionState.ERROR
                    }
                }
            }
            ?: emptyFlow()
    }

    override suspend fun close() {
        peerdroidConnector.close()
    }
}
