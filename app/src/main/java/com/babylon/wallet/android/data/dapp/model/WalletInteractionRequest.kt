package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionManifestData.TransactionMessage
import rdx.works.core.sargon.init

@Serializable
data class WalletInteraction(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("items")
    val items: WalletInteractionItems,
    @SerialName("metadata")
    val metadata: Metadata,
) : ConnectorExtensionInteraction {

    @Serializable
    data class Metadata(
        @SerialName("version")
        val version: Long,
        @SerialName("networkId")
        val networkId: Int,
        @SerialName("origin")
        val origin: String,
        @SerialName("dAppDefinitionAddress")
        val dAppDefinitionAddress: String,
    ) {

        companion object {
            const val VERSION = 2L
        }
    }
}

@Suppress("UnnecessaryAbstractClass")
@Serializable
sealed class WalletInteractionItems

@Serializable
@SerialName("unauthorizedRequest")
data class WalletUnauthorizedRequestItems(
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: PersonaDataRequestItem? = null, // Wallet clients should validate that `oneTimeAccounts.isOneTime == true`
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: AccountsRequestItem? = null // Wallet clients should validate that `oneTimeAccounts.isOneTime == true`
) : WalletInteractionItems()

@Serializable
@SerialName("authorizedRequest")
data class WalletAuthorizedRequestItems(
    @SerialName("auth")
    val auth: AuthRequestItem,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: AccountsRequestItem? = null,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: AccountsRequestItem? = null,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: PersonaDataRequestItem? = null,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: PersonaDataRequestItem? = null,
    @SerialName("reset")
    val reset: ResetRequestItem? = null
) : WalletInteractionItems()

@Serializable
@SerialName("transaction")
data class WalletTransactionItems(
    @SerialName("send")
    val send: SendTransactionItem,
) : WalletInteractionItems() {

    @Serializable
    data class SendTransactionItem(
        @SerialName("transactionManifest")
        val transactionManifest: String,
        @SerialName("version")
        val version: Long,
        @SerialName("blobs")
        val blobs: List<String>? = null,
        @SerialName("message")
        val message: String? = null
    )
}

fun WalletTransactionItems.SendTransactionItem.toDomainModel(
    remoteConnectorId: MessageFromDataChannel.RemoteEntityID, // from which CE comes the message
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    remoteConnectorId = remoteConnectorId,
    requestId = requestId,
    transactionManifestData = TransactionManifestData(
        instructions = transactionManifest,
        networkId = metadata.networkId,
        message = message?.let { TransactionMessage.Public(it) } ?: TransactionMessage.None,
        blobs = blobs?.map { it.hexToBagOfBytes() }.orEmpty(),
        version = version
    ),
    requestMetadata = metadata
)

fun WalletInteraction.toDomainModel(remoteEntityId: MessageFromDataChannel.RemoteEntityID): MessageFromDataChannel.IncomingRequest {
    try {
        val metadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            networkId = NetworkId.init(discriminant = metadata.networkId.toUByte()),
            origin = metadata.origin,
            dAppDefinitionAddress = metadata.dAppDefinitionAddress,
            isInternal = false
        )
        return when (items) {
            is WalletTransactionItems -> {
                items.send.toDomainModel(remoteEntityId, interactionId, metadata)
            }

            is WalletAuthorizedRequestItems -> {
                items.parseAuthorizedRequest(remoteEntityId, interactionId, metadata)
            }

            is WalletUnauthorizedRequestItems -> {
                items.parseUnauthorizedRequest(remoteEntityId, interactionId, metadata)
            }
        }
    } catch (e: Exception) {
        throw RadixWalletException.IncomingMessageException.MessageParse(e)
    }
}

private fun WalletUnauthorizedRequestItems.parseUnauthorizedRequest(
    remoteEntityId: MessageFromDataChannel.RemoteEntityID,
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest.UnauthorizedRequest {
    return MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = requestId,
        requestMetadata = metadata,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(),
    )
}

private fun WalletAuthorizedRequestItems.parseAuthorizedRequest(
    remoteEntityId: MessageFromDataChannel.RemoteEntityID,
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest {
    val auth = when (this.auth) {
        is AuthLoginRequestItem -> auth.toDomainModel()
        is AuthUsePersonaRequestItem -> auth.toDomainModel()
    }
    return MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteEntityId = remoteEntityId,
        interactionId = requestId,
        requestMetadata = metadata,
        authRequest = auth,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(isOngoing = false),
        ongoingAccountsRequestItem = ongoingAccounts?.toDomainModel(),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(isOngoing = false),
        ongoingPersonaDataRequestItem = ongoingPersonaData?.toDomainModel(),
        resetRequestItem = reset?.toDomainModel()
    )
}
