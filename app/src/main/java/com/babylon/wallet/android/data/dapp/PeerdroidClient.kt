package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.ConnectorExtensionExchangeInteraction
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionResponse
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.p2plink.RetiredP2PLinksRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingMessage
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.WalletToDappInteractionFailureResponse
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.shortId
import timber.log.Timber
import javax.inject.Inject

interface PeerdroidClient {

    val hasAtLeastOneConnection: Flow<Boolean>

    val openConnectionIds: Flow<Set<String>>

    /** Whether the connector of this link is connected, as it changes. */
    fun isConnectionOpen(remoteConnectorId: String): Flow<Boolean> =
        openConnectionIds.map { connectorIds -> remoteConnectorId in connectorIds }.distinctUntilChanged()

    suspend fun connect(
        p2pTransportProfile: P2pTransportProfile,
        connectionPassword: RadixConnectPassword
    ): Result<Unit>

    suspend fun sendMessage(
        remoteConnectorId: String,
        message: String
    ): Result<Unit>

    suspend fun sendMessage(
        message: String
    ): Result<Unit>

    fun listenForIncomingRequests(): Flow<DappToWalletInteraction>

    fun listenForLedgerResponses(): Flow<LedgerResponse>

    fun listenForIncomingRequestErrors(): Flow<IncomingMessage.Error>

    fun listenForLinkRemovals(): Flow<IncomingMessage.LinkRemoved>

    suspend fun deleteLink(connectionPassword: RadixConnectPassword)

    fun terminate()

    /**
     * The app came back to the foreground: the connections that can be checked quickly (iroh) are checked at once,
     * since they may have died while the app was frozen in the background.
     */
    fun onAppForeground()

    /**
     * The app went to the background: the iroh sessions stop after a grace period, until [onAppForeground].
     */
    fun onAppBackground()

    /** Whether the app is in front of the person (resumed); see [rdx.works.peerdroid.data.PeerdroidConnector.onAppInFront]. */
    fun onAppInFront(inFront: Boolean)
}

@Suppress("TooManyFunctions")
class PeerdroidClientImpl @Inject constructor(
    private val peerdroidConnector: PeerdroidConnector,
    private val retiredP2PLinksRepository: RetiredP2PLinksRepository,
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

    override fun isConnectionOpen(remoteConnectorId: String): Flow<Boolean> =
        peerdroidConnector.peerConnectionStatus
            .map { statuses -> statuses[remoteConnectorId] == PeerConnectionStatus.OPEN }
            .distinctUntilChanged()

    override suspend fun connect(
        p2pTransportProfile: P2pTransportProfile,
        connectionPassword: RadixConnectPassword
    ): Result<Unit> {
        return peerdroidConnector.connectToConnectorExtension(
            p2pTransportProfile = p2pTransportProfile,
            encryptionKey = connectionPassword
        )
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
                // The error type only, to keep log lines short.
                Timber.tag(LOG_TAG).e("🤖 stopped reading incoming messages: ${exception.javaClass.simpleName}")
            }
            .cancellable()
            .flowOn(ioDispatcher)
    }

    override fun listenForIncomingRequests(): Flow<DappToWalletInteraction> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForIncomingRequestErrors(): Flow<IncomingMessage.Error> {
        return listenForIncomingMessages().filterIsInstance()
    }

    override fun listenForLinkRemovals(): Flow<IncomingMessage.LinkRemoved> {
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

    override fun onAppForeground() {
        peerdroidConnector.onAppForeground()
    }

    override fun onAppBackground() {
        peerdroidConnector.onAppBackground()
    }

    override fun onAppInFront(inFront: Boolean) {
        peerdroidConnector.onAppInFront(inFront)
    }

    private suspend fun parseIncomingMessage(
        remoteConnectorId: String,
        messageInJsonString: String
    ): IncomingMessage {
        val link = connectorLogId(remoteConnectorId)
        return when {
            isLinkRemoved(messageInJsonString) -> {
                Timber.tag(LOG_TAG).i("🤖 $link the connector removed the link")
                IncomingMessage.LinkRemoved(remoteConnectorId)
            }

            // A link removed here only waits to tell its connector extension: nothing else it sends is handled (no
            // dApp request, no ledger response), and nothing is answered.
            retiredP2PLinksRepository.isRetired(remoteConnectorId) -> {
                Timber.tag(LOG_TAG).w("🤖 $link ignored a message (${messageInJsonString.length} chars): the link was removed")
                IncomingMessage.ParsingError
            }

            else -> parseDappOrLedgerMessage(remoteConnectorId, messageInJsonString)
        }
    }

    private suspend fun parseDappOrLedgerMessage(
        remoteConnectorId: String,
        messageInJsonString: String
    ): IncomingMessage {
        val link = connectorLogId(remoteConnectorId)
        // Try to parse as a dApp interaction
        val dAppInteraction = runCatching {
            DappToWalletInteractionUnvalidated.fromJson(messageInJsonString)
        }.getOrNull()

        if (dAppInteraction == null) {
            // Try to parse as a ledger interaction
            return parseLedgerInteraction(link, messageInJsonString).also { incomingMessage ->
                if (incomingMessage is LedgerResponse) {
                    Timber.tag(LOG_TAG).i("🤖 $link accepted a ledger response, interaction=${incomingMessage.id}")
                }
            }
        }

        // It's a dApp interaction, validate the version
        val interactionVersion = dAppInteraction.metadata.version.toLong()
        if (interactionVersion != Constants.WALLET_INTERACTION_VERSION) {
            val currentVersion = Constants.WALLET_INTERACTION_VERSION
            Timber.tag(LOG_TAG).w(
                "🤖 $link rejected interaction=${dAppInteraction.interactionId}: version $interactionVersion is incompatible " +
                    "with the wallet's $currentVersion, answering with a failure"
            )
            sendFailureResponseToDApp(
                requestId = dAppInteraction.interactionId,
                remoteConnectorId = remoteConnectorId,
                dAppWalletInteractionErrorType = DappWalletInteractionErrorType.INCOMPATIBLE_VERSION,
                message = null
            )

            return IncomingMessage.ParsingError
        }

        // Process the unvalidated dApp to wallet interaction
        return dAppInteraction.toDomainModel(
            remoteEntityId = RemoteEntityID.ConnectorId(remoteConnectorId)
        ).fold(
            onSuccess = {
                Timber.tag(LOG_TAG).i("🤖 $link accepted a dApp request, interaction=${dAppInteraction.interactionId}")
                it
            },
            onFailure = { error ->
                Timber.tag(LOG_TAG).w(
                    "🤖 $link rejected interaction=${dAppInteraction.interactionId}: invalid request (${error.javaClass.simpleName})"
                )
                when (error) {
                    is RadixWalletException -> IncomingMessage.Error(error)
                    else -> IncomingMessage.ParsingError
                }
            }
        )
    }

    // Exactly `{"discriminator":"linkRemoved"}`: a message with anything else in it (e.g. a dApp request that happens to
    // carry that discriminator) is not a removal, as the app's lenient Json would otherwise decode it as one.
    private fun isLinkRemoved(messageInJsonString: String): Boolean = runCatching {
        json.parseToJsonElement(messageInJsonString).jsonObject.keys == setOf(DISCRIMINATOR_KEY) &&
            json.decodeFromString<ConnectorExtensionExchangeInteraction>(messageInJsonString) ==
            ConnectorExtensionExchangeInteraction.LinkRemoved
    }.getOrDefault(false)

    // The error types only, to keep log lines short.
    private fun parseLedgerInteraction(link: String, messageInJsonString: String): IncomingMessage = runCatching {
        val interaction = json.decodeFromString<LedgerInteractionResponse>(messageInJsonString)
        interaction.toDomainModel()
    }.fold(
        onSuccess = { it },
        onFailure = { error ->
            when (error) {
                is SerializationException -> {
                    Timber.tag(LOG_TAG).w(
                        "🤖 $link rejected a message (${messageInJsonString.length} chars): neither a dApp request nor a ledger " +
                            "response (${error.javaClass.simpleName})"
                    )
                    IncomingMessage.ParsingError
                }

                else -> {
                    Timber.tag(LOG_TAG).w("🤖 $link rejected a ledger response: ${error.javaClass.simpleName}")
                    IncomingMessage.Error(RadixWalletException.DappRequestException.InvalidRequestChallenge)
                }
            }
        }
    )

    // The short form of the connector's id (a hash of the link's password): never the full id.
    private fun connectorLogId(remoteConnectorId: String) = "link=${ConnectionIdHolder(remoteConnectorId).shortId}"

    private companion object {
        const val LOG_TAG = "RadixConnect"
        const val DISCRIMINATOR_KEY = "discriminator"
    }

    private suspend fun sendFailureResponseToDApp(
        remoteConnectorId: String,
        requestId: String,
        dAppWalletInteractionErrorType: DappWalletInteractionErrorType,
        message: String?
    ) {
        val messageJson = WalletToDappInteractionResponse.Failure(
            WalletToDappInteractionFailureResponse(
                interactionId = requestId,
                error = dAppWalletInteractionErrorType,
                message = message
            )
        ).toJson()
        sendMessage(remoteConnectorId, messageJson)
    }
}
