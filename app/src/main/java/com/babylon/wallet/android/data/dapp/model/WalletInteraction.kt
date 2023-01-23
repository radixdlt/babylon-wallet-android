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
        @SerialName("dAppId")
        val dAppId: String,
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
    val ongoingPersonaData: OngoingPersonaDataRequestItem? = null
) : WalletInteractionItems()

private val walletRequestSerializersModule = SerializersModule {
    polymorphic(OneTimeAccountsRequestResponseItem::class) {
        subclass(
            OneTimeAccountsWithProofOfOwnershipRequestResponseItem::class,
            OneTimeAccountsWithProofOfOwnershipRequestResponseItem.serializer()
        )
        subclass(
            OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem::class,
            OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem.serializer()
        )
    }
    polymorphic(OngoingAccountsRequestResponseItem::class) {
        subclass(
            OngoingAccountsWithProofOfOwnershipRequestResponseItem::class,
            OngoingAccountsWithProofOfOwnershipRequestResponseItem.serializer()
        )
        subclass(
            OngoingAccountsWithoutProofOfOwnershipRequestResponseItem::class,
            OngoingAccountsWithoutProofOfOwnershipRequestResponseItem.serializer()
        )
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
}

val walletRequestJson = Json {
    serializersModule = walletRequestSerializersModule
    classDiscriminator = "discriminator"
}

fun WalletInteractionItems.toDomainModel(requestId: String, networkId: Int): MessageFromDataChannel.IncomingRequest {
    return when (this) {
        is SendTransactionItem -> toDomainModel(requestId, networkId)
        is WalletTransactionItems -> this.send.toDomainModel(requestId, networkId)
        is WalletAuthorizedRequestItems -> {
            parseAuthorizedRequest(requestId)
        }
        is WalletUnauthorizedRequestItems -> {
            parseUnauthorizedRequest(requestId)
        }
    }
}

private fun WalletUnauthorizedRequestItems.parseUnauthorizedRequest(
    requestId: String
) = when {
    this.oneTimeAccounts != null -> {
        oneTimeAccounts.toDomainModel(requestId)
    }
    this.oneTimePersonaData != null -> {
        oneTimePersonaData.toDomainModel(requestId)
    }
    else -> MessageFromDataChannel.IncomingRequest.Unknown
}

private fun WalletAuthorizedRequestItems.parseAuthorizedRequest(
    requestId: String,
): MessageFromDataChannel.IncomingRequest {
    val auth = when (this.auth) {
        is AuthLoginRequestItem -> auth.toDomainModel()
        is AuthUsePersonaRequestItem -> auth.toDomainModel()
    }
    return when {
        this.oneTimeAccounts != null -> {
            oneTimeAccounts.toDomainModel(requestId, auth)
        }
        this.oneTimePersonaData != null -> {
            oneTimePersonaData.toDomainModel(requestId, auth)
        }
        this.ongoingAccounts != null -> {
            ongoingAccounts.toDomainModel(requestId, auth)
        }
        this.ongoingPersonaData != null -> {
            ongoingPersonaData.toDomainModel(requestId, auth)
        }
        else -> MessageFromDataChannel.IncomingRequest.Unknown
    }
}
