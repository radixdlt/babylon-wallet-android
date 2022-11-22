package com.babylon.wallet.android.data.dapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber

interface PeerdroidClient {

    suspend fun connectToRemoteClientWithEncryptionKey(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendMessage(message: String): Result<Unit>

    fun listenForEvents(): Flow<DataChannelEvent>

    suspend fun close()
}

class PeerdroidClientImpl(
    private val peerdroidConnector: PeerdroidConnector
) : PeerdroidClient {

    private var dataChannel: DataChannelWrapper? = null

    override suspend fun connectToRemoteClientWithEncryptionKey(
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
                Result.Error("data channel failed to initialize")
            }
        }
    }

    override suspend fun sendMessage(message: String): Result<Unit> {
        return dataChannel
            ?.sendMessage(message)
            ?: Result.Error("data channel is null")
    }

    override fun listenForEvents(): Flow<DataChannelEvent> {
        return dataChannel
            ?.dataChannelEvents
            ?.onEach { dataChannelEvent ->
                if (dataChannelEvent is DataChannelEvent.UnknownError) {
                    Timber.e("an unknown error occurred: ${dataChannelEvent.message}")
                }
            }
            ?: emptyFlow()
    }

    override suspend fun close() {
        peerdroidConnector.close()
    }
}
