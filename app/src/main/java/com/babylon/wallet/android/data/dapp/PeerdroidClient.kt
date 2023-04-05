package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.dapp.model.walletRequestJson
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import com.radixdlt.hex.extensions.toHexString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import rdx.works.core.sha256Hash
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent.IncomingMessage
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent.StateChanged
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    suspend fun connect(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendMessage(
        remoteClientId: String,
        message: String
    ): Result<Unit>

    fun listenForIncomingRequests(): Flow<MessageFromDataChannel>

    suspend fun deleteLink(connectionPassword: String)

    fun terminate()
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidClient {

    override suspend fun connect(encryptionKey: ByteArray): Result<Unit> {
        return peerdroidConnector.connectToConnectorExtension(encryptionKey = encryptionKey)
    }

    override suspend fun sendMessage(
        remoteClientId: String,
        message: String
    ): Result<Unit> {
        return peerdroidConnector.sendDataChannelMessageToRemoteClient(
            remoteClientId = remoteClientId,
            message = message
        )
    }

    override fun listenForIncomingRequests(): Flow<MessageFromDataChannel> {
        return peerdroidConnector.dataChannelMessagesFromRemoteClients
            .filter { dataChannelEvent ->
                dataChannelEvent is StateChanged || dataChannelEvent is IncomingMessage.DecodedMessage
            }
            .map { dataChannelEvent ->
                when (dataChannelEvent) {
                    is StateChanged -> { // TODO no need here
                        parseDataChannelState(dataChannelEvent)
                    }
                    is IncomingMessage.DecodedMessage -> {
                        parseIncomingMessage(
                            remoteClientId = dataChannelEvent.remoteClientId,
                            messageInJsonString = dataChannelEvent.messageInJsonString
                        )
                    }
                    else -> {
                        MessageFromDataChannel.None
                    }
                }
            }.catch { exception ->
                Timber.e("caught exception: ${exception.localizedMessage}")
                if (exception is SerializationException) {
                    emit(MessageFromDataChannel.ParsingError)
                } else {
                    throw exception
                }
            }
            .cancellable()
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteLink(connectionPassword: String) {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = connectionPassword
        )
        encryptionKey?.let {
            val connectionIdHolder = ConnectionIdHolder(id = it.sha256Hash().toHexString())
            peerdroidConnector.deleteConnector(connectionIdHolder)
        } ?: Timber.e("Failed to close peer connection because connection password is wrong")
    }

    override fun terminate() {
        peerdroidConnector.terminateConnectionToConnectorExtension()
    }

    private fun parseDataChannelState(stateChanged: StateChanged): MessageFromDataChannel.ConnectionStateChanged {
        return when (stateChanged) {
            StateChanged.CONNECTING -> {
                MessageFromDataChannel.ConnectionStateChanged.CONNECTING
            }
            StateChanged.OPEN -> {
                MessageFromDataChannel.ConnectionStateChanged.OPEN
            }
            StateChanged.CLOSING -> {
                MessageFromDataChannel.ConnectionStateChanged.CLOSING
            }
            StateChanged.CLOSE -> {
                MessageFromDataChannel.ConnectionStateChanged.CLOSE
            }
            StateChanged.DELETE_CONNECTION -> {
                MessageFromDataChannel.ConnectionStateChanged.DELETE_CONNECTION
            }
            StateChanged.UNKNOWN -> {
                MessageFromDataChannel.ConnectionStateChanged.ERROR
            }
        }
    }

    private fun parseIncomingMessage(
        remoteClientId: String,
        messageInJsonString: String
    ): MessageFromDataChannel.IncomingRequest {
        val request = walletRequestJson.decodeFromString<WalletInteraction>(messageInJsonString)
        return request.toDomainModel(dappId = remoteClientId)
    }
}
