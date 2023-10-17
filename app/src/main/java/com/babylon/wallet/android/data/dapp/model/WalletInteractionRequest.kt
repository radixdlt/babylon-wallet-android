package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.hex.decode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    remoteConnectorId: String, // from which CE comes the message
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
) =
    MessageFromDataChannel.IncomingRequest.TransactionRequest(
        remoteConnectorId = remoteConnectorId,
        requestId = requestId,
        transactionManifestData = TransactionManifestData(
            transactionManifest,
            version,
            metadata.networkId,
            blobs?.map { decode(it) }.orEmpty(),
            message = message
        ),
        requestMetadata = metadata
    )

fun WalletInteraction.toDomainModel(remoteConnectorId: String): MessageFromDataChannel.IncomingRequest {
    try {
        val metadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
            networkId = metadata.networkId,
            origin = metadata.origin,
            dAppDefinitionAddress = metadata.dAppDefinitionAddress,
            isInternal = false
        )
        return when (items) {
            is WalletTransactionItems -> {
                items.send.toDomainModel(remoteConnectorId, interactionId, metadata)
            }
            is WalletAuthorizedRequestItems -> {
                items.parseAuthorizedRequest(remoteConnectorId, interactionId, metadata)
            }
            is WalletUnauthorizedRequestItems -> {
                items.parseUnauthorizedRequest(remoteConnectorId, interactionId, metadata)
            }
        }
    } catch (e: Exception) {
        throw RadixWalletException.IncomingMessageException.MessageParseException(e)
    }
}

private fun WalletUnauthorizedRequestItems.parseUnauthorizedRequest(
    remoteConnectorId: String,
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest.UnauthorizedRequest {
    return MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
        remoteConnectorId = remoteConnectorId,
        interactionId = requestId,
        requestMetadata = metadata,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(),
        oneTimePersonaDataRequestItem = oneTimePersonaData?.toDomainModel(),
    )
}

private fun WalletAuthorizedRequestItems.parseAuthorizedRequest(
    remoteConnectorId: String,
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest {
    val auth = when (this.auth) {
        is AuthLoginRequestItem -> auth.toDomainModel()
        is AuthUsePersonaRequestItem -> auth.toDomainModel()
    }
    return MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        remoteConnectorId = remoteConnectorId,
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
