package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class WalletRequest(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("items")
    val items: List<WalletRequestItem>,
    @SerialName("metadata")
    val metadata: Metadata
) {

    @Serializable
    data class Metadata(
        @SerialName("networkId")
        val networkId: Int,
        @SerialName("origin")
        val origin: String,
        @SerialName("dAppId")
        val dAppId: String
    )
}

@Suppress("UnnecessaryAbstractClass")
@Polymorphic
@Serializable
abstract class WalletRequestItem

private val walletRequestSerializersModule = SerializersModule {
    polymorphic(WalletRequestItem::class) {
        subclass(OneTimeAccountsReadRequestItem::class, OneTimeAccountsReadRequestItem.serializer())
        subclass(OngoingAccountsReadRequestItem::class, OngoingAccountsReadRequestItem.serializer())

        subclass(OneTimePersonaDataReadRequestItem::class, OneTimePersonaDataReadRequestItem.serializer())
        subclass(OngoingPersonaDataReadRequestItem::class, OngoingPersonaDataReadRequestItem.serializer())
        subclass(UsePersonaReadRequestItem::class, UsePersonaReadRequestItem.serializer())

        subclass(LoginReadRequestItem::class, LoginReadRequestItem.serializer())
        subclass(SendTransactionWriteRequestItem::class, SendTransactionWriteRequestItem.serializer())
    }
}

val walletRequestJson = Json {
    serializersModule = walletRequestSerializersModule
    classDiscriminator = "requestType"
}

fun WalletRequestItem.toDomainModel(requestId: String, networkId: Int): MessageFromDataChannel.IncomingRequest {
    return when (this) {
        is OneTimeAccountsReadRequestItem -> toDomainModel(requestId)
        is OngoingAccountsReadRequestItem -> toDomainModel(requestId)
        is SendTransactionWriteRequestItem -> toDomainModel(requestId, networkId)
        else -> MessageFromDataChannel.IncomingRequest.Unknown
    }
}
