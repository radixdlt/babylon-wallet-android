package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.IncompatibleRequestVersionException
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionResponse
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingMessage
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
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

    fun listenForIncomingRequests(): Flow<DappToWalletInteraction>

    fun listenForLedgerResponses(): Flow<LedgerResponse>

    fun listenForIncomingRequestErrors(): Flow<Error>

    suspend fun deleteLink(connectionPassword: RadixConnectPassword)

    fun terminate()
}

class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
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

    override fun listenForIncomingRequests(): Flow<DappToWalletInteraction> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForIncomingRequestErrors(): Flow<Error> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForLedgerResponses(): Flow<LedgerResponse> {
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
            val dappInteraction = runCatching {
                DappToWalletInteractionUnvalidated.fromJson(messageInJsonString)
            }.getOrNull()
            if (dappInteraction != null) {
                val interactionVersion = dappInteraction.metadata.version.toLong()
                if (interactionVersion != Constants.WALLET_INTERACTION_VERSION) {
                    throw IncompatibleRequestVersionException(
                        requestVersion = interactionVersion,
                        requestId = dappInteraction.interactionId
                    )
                }
                dappInteraction.toDomainModel(remoteEntityId = RemoteEntityID.ConnectorId(remoteConnectorId)).getOrThrow()
            } else {
                val interaction = json.decodeFromString<LedgerInteractionResponse>(messageInJsonString)
                interaction.toDomainModel()
            }
        } catch (serializationException: SerializationException) {
            // TODO a snackbar message error like iOS
            Timber.e("failed to parse incoming message with serialization exception: ${serializationException.localizedMessage}")
            IncomingMessage.ParsingError
        } catch (incompatibleRequestVersionException: IncompatibleRequestVersionException) {
            val requestVersion = incompatibleRequestVersionException.requestVersion
            val currentVersion = Constants.WALLET_INTERACTION_VERSION
            Timber.e("The version of the request: $requestVersion is incompatible. Wallet version: $currentVersion")
            sendIncompatibleVersionRequestToDapp(
                requestId = incompatibleRequestVersionException.requestId,
                remoteConnectorId = remoteConnectorId
            )
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
                    interactionId = requestId,
                    error = DappWalletInteractionErrorType.INCOMPATIBLE_VERSION,
                    message = null
                )
            ).toJson()
        sendMessage(remoteConnectorId, messageJson)
    }
}
