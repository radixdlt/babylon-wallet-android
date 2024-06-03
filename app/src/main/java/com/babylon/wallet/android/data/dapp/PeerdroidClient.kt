package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionInteraction
import com.babylon.wallet.android.data.dapp.model.IncompatibleRequestVersionException
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionResponse
import com.babylon.wallet.android.data.dapp.model.asJsonString
import com.babylon.wallet.android.data.dapp.model.init
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
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

    fun listenForIncomingRequests(): Flow<IncomingMessage.IncomingRequest>

    fun listenForLedgerResponses(): Flow<IncomingMessage.LedgerResponse>

    fun listenForIncomingRequestErrors(): Flow<IncomingMessage.Error>

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

    private fun listenForIncomingMessages(): Flow<IncomingMessage> {
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

    override fun listenForIncomingRequests(): Flow<IncomingMessage.IncomingRequest> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForIncomingRequestErrors(): Flow<IncomingMessage.Error> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForLedgerResponses(): Flow<IncomingMessage.LedgerResponse> {
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
    ): IncomingMessage {
        return try {
            val dappInteraction = DappToWalletInteractionUnvalidated.Companion.init(messageInJsonString).getOrNull()
            if (dappInteraction != null) {
                dappInteraction.toDomainModel(remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId(remoteConnectorId))
            } else {
                val interaction = peerdroidRequestJson.decodeFromString<ConnectorExtensionInteraction>(messageInJsonString)
                (interaction as? LedgerInteractionResponse)?.toDomainModel() ?: IncomingMessage.ParsingError
            }
        } catch (serializationException: SerializationException) {
            // TODO a snackbar message error like iOS
            Timber.e("failed to parse incoming message with serialization exception: ${serializationException.localizedMessage}")
            IncomingMessage.ParsingError
        } catch (incompatibleRequestVersionException: IncompatibleRequestVersionException) {
//            val requestVersion = incompatibleRequestVersionException.requestVersion
//            val currentVersion = WalletInteraction.Metadata.VERSION
//            Timber.e("The version of the request: $requestVersion is incompatible. Wallet version: $currentVersion")
//            sendIncompatibleVersionRequestToDapp(
//                requestId = incompatibleRequestVersionException.requestId,
//                remoteConnectorId = remoteConnectorId
//            ) TODO revisit after model update
            IncomingMessage.ParsingError
        } catch (e: RadixWalletException.IncomingMessageException.MessageParse) {
            IncomingMessage.Error(e)
        } catch (e: RadixWalletException.IncomingMessageException.LedgerResponseParse) {
            IncomingMessage.Error(e)
        } catch (exception: Exception) {
            Timber.e("failed to parse incoming message: ${exception.localizedMessage}")
            IncomingMessage.Error(RadixWalletException.IncomingMessageException.Unknown(exception))
        }
    }

    private suspend fun sendIncompatibleVersionRequestToDapp(
        remoteConnectorId: String,
        requestId: String
    ) {
        val messageJson =
            WalletToDappInteractionResponse.Failure(
                WalletToDappInteractionFailureResponse(
                    interactionId = WalletInteractionId.fromString(requestId),
                    error = DappWalletInteractionErrorType.INCOMPATIBLE_VERSION,
                    message = null
                )
            ).asJsonString().getOrThrow() // TODO revisit after sargon change
        sendMessage(remoteConnectorId, messageJson)
    }
}
