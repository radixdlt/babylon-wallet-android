package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionInteraction
import com.babylon.wallet.android.data.dapp.model.IncompatibleRequestVersionException
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.WalletInteractionFailureResponse
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.RadixWalletException
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        remoteConnectorId: String,
        message: String
    ): Result<Unit>

    suspend fun sendMessage(
        message: String
    ): Result<Unit>

    fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest>

    suspend fun deleteLink(connectionPassword: String)

    fun terminate()
    fun listenForLedgerResponses(): Flow<MessageFromDataChannel.LedgerResponse>
    fun listenForIncomingRequestErrors(): Flow<MessageFromDataChannel.Error.DappRequest>
    val anyChannelConnected: Flow<Boolean>
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidClient {

    override suspend fun connect(encryptionKey: ByteArray): Result<Unit> {
        return peerdroidConnector.connectToConnectorExtension(encryptionKey = encryptionKey)
    }

    override suspend fun sendMessage(
        remoteConnectorId: String,
        message: String
    ): Result<Unit> {
        return peerdroidConnector.sendDataChannelMessageToRemoteClient(
            remoteConnectorId = remoteConnectorId,
            message = message
        )
    }

    override suspend fun sendMessage(message: String): Result<Unit> {
        return peerdroidConnector.sendDataChannelMessageToAllRemoteClients(message)
    }

    private fun listenForIncomingMessages(): Flow<MessageFromDataChannel> {
        return peerdroidConnector
            .dataChannelMessagesFromRemoteClients
            .filterIsInstance<DataChannelWrapperEvent.MessageFromRemoteConnectionId>()
            .map { messageFromRemoteClient ->
                parseIncomingMessage(
                    remoteConnectorId = messageFromRemoteClient.connectionIdHolder.id,
                    messageInJsonString = messageFromRemoteClient.messageInJsonString
                )
            }.catch { exception ->
                Timber.e("caught exception: ${exception.localizedMessage}")
            }
            .cancellable()
            .flowOn(ioDispatcher)
    }

    override fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForIncomingRequestErrors(): Flow<MessageFromDataChannel.Error.DappRequest> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override val anyChannelConnected: Flow<Boolean>
        get() = peerdroidConnector.anyChannelConnected

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

    @Suppress("SwallowedException")
    private suspend fun parseIncomingMessage(
        remoteConnectorId: String,
        messageInJsonString: String
    ): MessageFromDataChannel {
        return try {
            when (val payload = peerdroidRequestJson.decodeFromString<ConnectorExtensionInteraction>(messageInJsonString)) {
                is WalletInteraction -> payload.toDomainModel(remoteConnectorId = remoteConnectorId)
                else -> (payload as LedgerInteractionResponse).toDomainModel()
            }
        } catch (serializationException: SerializationException) {
            // TODO a snackbar message error like iOS
            Timber.e("failed to parse incoming message with serialization exception: ${serializationException.localizedMessage}")
            MessageFromDataChannel.ParsingError
        } catch (incompatibleRequestVersionException: IncompatibleRequestVersionException) {
            val requestVersion = incompatibleRequestVersionException.requestVersion
            val currentVersion = WalletInteraction.Metadata.VERSION
            Timber.e("The version of the request: $requestVersion is incompatible. Wallet version: $currentVersion")
            sendIncompatibleVersionRequestToDapp(
                requestId = incompatibleRequestVersionException.requestId,
                remoteConnectorId = remoteConnectorId
            )
            MessageFromDataChannel.ParsingError
        } catch (e: RadixWalletException.ErrorParsingIncomingRequest) {
            MessageFromDataChannel.Error.DappRequest
        } catch (e: RadixWalletException.ErrorParsingLedgerResponse) {
            MessageFromDataChannel.Error.LedgerResponse
        } catch (exception: Exception) {
            Timber.e("failed to parse incoming message: ${exception.localizedMessage}")
            MessageFromDataChannel.Error.Unknown
        }
    }

    private suspend fun sendIncompatibleVersionRequestToDapp(
        remoteConnectorId: String,
        requestId: String
    ) {
        val messageJson = Json.encodeToString(
            WalletInteractionFailureResponse(
                interactionId = requestId,
                error = WalletErrorType.IncompatibleVersion
            )
        )
        sendMessage(remoteConnectorId, messageJson)
    }
}
