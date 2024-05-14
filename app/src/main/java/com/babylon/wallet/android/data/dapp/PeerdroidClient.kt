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
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.PeerConnectionStatus
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    val hasAtLeastOneConnection: Flow<Boolean>

    val openConnectionIds: Flow<Set<String>>

    suspend fun connect(connectionPassword: RadixConnectPassword): Result<Unit>

    suspend fun sendMessage(
        remoteConnectorId: String,
        message: String
    ): Result<Unit>

    suspend fun sendMessage(
        message: String
    ): Result<Unit>

    fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest>

    fun listenForLedgerResponses(): Flow<MessageFromDataChannel.LedgerResponse>

    fun listenForIncomingRequestErrors(): Flow<MessageFromDataChannel.Error>

    suspend fun deleteLink(connectionPassword: RadixConnectPassword)

    fun terminate()
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidClient {

    override val hasAtLeastOneConnection: Flow<Boolean>
        get() = peerdroidConnector.anyChannelConnected

    override val openConnectionIds: Flow<Set<String>>
        get() = peerdroidConnector.peerConnectionStatus
            .filter { connectionStatuses ->
                connectionStatuses.isNotEmpty() &&
                    !connectionStatuses.any { it.value == PeerConnectionStatus.CONNECTING }
            }
            .map { statuses ->
                statuses.filter { it.value == PeerConnectionStatus.OPEN }.keys
            }

    override suspend fun connect(connectionPassword: RadixConnectPassword): Result<Unit> {
        return peerdroidConnector.connectToConnectorExtension(encryptionKey = connectionPassword)
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
                Timber.e("\uD83E\uDD16 caught exception: ${exception.localizedMessage}")
            }
            .cancellable()
            .flowOn(ioDispatcher)
    }

    override fun listenForIncomingRequests(): Flow<MessageFromDataChannel.IncomingRequest> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForIncomingRequestErrors(): Flow<MessageFromDataChannel.Error> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForLedgerResponses(): Flow<MessageFromDataChannel.LedgerResponse> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override suspend fun deleteLink(connectionPassword: RadixConnectPassword) {
        peerdroidConnector.deleteConnector(ConnectionIdHolder(password = connectionPassword))
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
        } catch (e: RadixWalletException.IncomingMessageException.MessageParse) {
            MessageFromDataChannel.Error(e)
        } catch (e: RadixWalletException.IncomingMessageException.LedgerResponseParse) {
            MessageFromDataChannel.Error(e)
        } catch (exception: Exception) {
            Timber.e("failed to parse incoming message: ${exception.localizedMessage}")
            MessageFromDataChannel.Error(RadixWalletException.IncomingMessageException.Unknown(exception))
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
