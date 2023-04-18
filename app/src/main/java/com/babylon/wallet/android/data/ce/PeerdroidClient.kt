package com.babylon.wallet.android.data.ce

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.parseEncryptionKeyFromConnectionPassword
import com.radixdlt.hex.extensions.toHexString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import rdx.works.core.blake2Hash
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    suspend fun connect(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendMessage(
        remoteClientId: String,
        message: String
    ): Result<Unit>

    suspend fun sendMessage(
        message: String
    ): Result<Unit>

    fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest>

    suspend fun deleteLink(connectionPassword: String)

    fun terminate()
    fun listenForLedgerResponses(): Flow<MessageFromDataChannel.LedgerResponse>
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

    override suspend fun sendMessage(message: String): Result<Unit> {
        return peerdroidConnector.sendDataChannelMessageToRemoteClients(message)
    }

    private fun listenForIncomingMessages(): Flow<MessageFromDataChannel> {
        return peerdroidConnector
            .dataChannelMessagesFromRemoteClients
            .filterIsInstance<DataChannelWrapperEvent.MessageFromRemoteClient>()
            .map { messageFromRemoteClient ->
                parseIncomingMessage(
                    remoteClientId = messageFromRemoteClient.remoteClientId,
                    messageInJsonString = messageFromRemoteClient.messageInJsonString
                )
            }.catch { exception ->
                Timber.e("caught exception: ${exception.localizedMessage}")
                // TODO a snackbar message error? close the data channel between wallet and dapp?
            }
            .cancellable()
            .flowOn(ioDispatcher)
    }

    override fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForLedgerResponses(): Flow<MessageFromDataChannel.LedgerResponse> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override suspend fun deleteLink(connectionPassword: String) {
        val encryptionKey = parseEncryptionKeyFromConnectionPassword(
            connectionPassword = connectionPassword
        )
        encryptionKey?.let {
            val connectionIdHolder = ConnectionIdHolder(id = it.blake2Hash().toHexString())
            peerdroidConnector.deleteConnector(connectionIdHolder)
        } ?: Timber.e("Failed to close peer connection because connection password is wrong")
    }

    override fun terminate() {
        peerdroidConnector.terminateConnectionToConnectorExtension()
    }

    private fun parseIncomingMessage(
        remoteClientId: String,
        messageInJsonString: String
    ): MessageFromDataChannel {
        val payload = peerdroidRequestJson.decodeFromString<ConnectorExtensionInteraction>(messageInJsonString)
        return when (payload) {
            is WalletInteraction -> payload.toDomainModel(dappId = remoteClientId)
            else -> (payload as LedgerInteractionResponse).toDomainModel()
        }
    }
}
