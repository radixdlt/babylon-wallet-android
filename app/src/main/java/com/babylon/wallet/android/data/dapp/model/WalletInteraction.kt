package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class WalletInteraction(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("items")
    val items: WalletInteractionItems,
    @SerialName("metadata")
    val metadata: Metadata,
) {

    @Serializable
    data class Metadata(
        @SerialName("networkId")
        val networkId: Int,
        @SerialName("origin")
        val origin: String,
        @SerialName("dAppDefinitionAddress")
        val dAppDefinitionAddress: String,
    )
}

@Suppress("UnnecessaryAbstractClass")
@Serializable
sealed class WalletInteractionItems

@Serializable
@SerialName("transaction")
data class WalletTransactionItems(
    @SerialName("send")
    val send: SendTransactionItem,
) : WalletInteractionItems()

@Serializable
@SerialName("unauthorizedRequest")
data class WalletUnauthorizedRequestItems(
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestItem? = null,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestItem? = null
) : WalletInteractionItems()

@Serializable
@SerialName("authorizedRequest")
data class WalletAuthorizedRequestItems(
    @SerialName("auth")
    val auth: AuthRequestItem,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestItem? = null,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: OngoingAccountsRequestItem? = null,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestItem? = null,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: OngoingPersonaDataRequestItem? = null,
    @SerialName("reset")
    val reset: ResetRequestItem? = null
) : WalletInteractionItems()

private val walletRequestSerializersModule = SerializersModule {
    polymorphic(WalletInteractionResponseItems::class) {
        subclass(WalletUnauthorizedRequestResponseItems::class, WalletUnauthorizedRequestResponseItems.serializer())
        subclass(WalletAuthorizedRequestResponseItems::class, WalletAuthorizedRequestResponseItems.serializer())
        subclass(WalletTransactionResponseItems::class, WalletTransactionResponseItems.serializer())
    }
    polymorphic(AuthRequestItem::class) {
        subclass(AuthUsePersonaRequestItem::class, AuthUsePersonaRequestItem.serializer())
        subclass(AuthLoginRequestItem::class, AuthLoginRequestItem.serializer())
    }
    polymorphic(AuthRequestResponseItem::class) {
        subclass(
            AuthLoginWithChallengeRequestResponseItem::class,
            AuthLoginWithChallengeRequestResponseItem.serializer()
        )
        subclass(
            AuthLoginWithoutChallengeRequestResponseItem::class,
            AuthLoginWithoutChallengeRequestResponseItem.serializer()
        )
        subclass(
            AuthUsePersonaRequestResponseItem::class,
            AuthUsePersonaRequestResponseItem.serializer()
        )
    }
    polymorphic(WalletInteractionItems::class) {
        subclass(WalletUnauthorizedRequestItems::class, WalletUnauthorizedRequestItems.serializer())
        subclass(WalletAuthorizedRequestItems::class, WalletAuthorizedRequestItems.serializer())
        subclass(WalletTransactionItems::class, WalletTransactionItems.serializer())
    }
    polymorphic(WalletInteractionResponse::class) {
        subclass(WalletInteractionSuccessResponse::class, WalletInteractionSuccessResponse.serializer())
        subclass(WalletInteractionFailureResponse::class, WalletInteractionFailureResponse.serializer())
    }
}

val walletRequestJson = Json {
    serializersModule = walletRequestSerializersModule
    classDiscriminator = "discriminator"
}

fun WalletInteraction.toDomainModel(): MessageFromDataChannel.IncomingRequest {
    val metadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
        metadata.networkId,
        metadata.origin,
        metadata.dAppDefinitionAddress
    )
    return when (items) {
        is WalletTransactionItems -> items.send.toDomainModel(interactionId, metadata)
        is WalletAuthorizedRequestItems -> {
            items.parseAuthorizedRequest(interactionId, metadata)
        }
        is WalletUnauthorizedRequestItems -> {
            items.parseUnauthorizedRequest(interactionId, metadata)
        }
    }
}

private fun WalletUnauthorizedRequestItems.parseUnauthorizedRequest(
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest.UnauthorizedRequest {
    return MessageFromDataChannel.IncomingRequest.UnauthorizedRequest(
        requestId,
        metadata,
        oneTimeAccounts?.toDomainModel(),
        oneTimePersonaData?.toDomainModel(),
    )
}

private fun WalletAuthorizedRequestItems.parseAuthorizedRequest(
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
): MessageFromDataChannel.IncomingRequest {
    val auth = when (this.auth) {
        is AuthLoginRequestItem -> auth.toDomainModel()
        is AuthUsePersonaRequestItem -> auth.toDomainModel()
    }
    return MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
        requestId = requestId,
        requestMetadata = metadata,
        authRequest = auth,
        oneTimeAccountsRequestItem = oneTimeAccounts?.toDomainModel(),
        ongoingAccountsRequestItem = ongoingAccounts?.toDomainModel(),
        oneTimePersonaRequestItem = oneTimePersonaData?.toDomainModel(),
        ongoingPersonaRequestItem = ongoingPersonaData?.toDomainModel(),
        resetRequestItem = reset?.toDomainModel()
    )
}
