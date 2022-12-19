package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsReadRequestItem
import com.babylon.wallet.android.data.dapp.model.WalletRequest
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.dapp.model.walletRequestJson
import com.babylon.wallet.android.domain.model.ConnectionState
import com.babylon.wallet.android.domain.model.IncomingRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
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

    fun listenForStateEvents(): Flow<ConnectionState>

    fun listenForIncomingRequests(): Flow<IncomingRequest>

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

    override fun listenForStateEvents(): Flow<ConnectionState> {
        return dataChannel
            ?.dataChannelEvents
            ?.cancellable()
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

    override fun listenForIncomingRequests(): Flow<IncomingRequest> {
        return dataChannel
            ?.dataChannelEvents
            ?.cancellable()
            ?.filterIsInstance<DataChannelEvent.IncomingMessage.DecodedMessage>()
            ?.map { decodedMessage ->
                parseIncomingMessage(messageInJsonString = decodedMessage.message)
            }
            ?: emptyFlow()
    }

    private fun parseIncomingMessage(messageInJsonString: String): IncomingRequest {
        val request = walletRequestJson.decodeFromString<WalletRequest>(messageInJsonString)
        val requestId = request.requestId
        val walletRequestItemsList = request.items

        // TODO later we should implement this in a more elegant way by parsing any kind of request
        return if (walletRequestItemsList.firstOrNull() is OneTimeAccountsReadRequestItem) {
            val accountsReadRequest = walletRequestItemsList[0]
            (accountsReadRequest as OneTimeAccountsReadRequestItem).toDomainModel(requestId)
        } else {
            IncomingRequest.SomeOtherRequest
        }
    }

    override suspend fun close() {
        dataChannel?.close()
        dataChannel = null
        peerdroidConnector.close()
    }
}
