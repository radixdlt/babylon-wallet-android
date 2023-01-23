package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class AuthLoginRequestItem(
    @SerialName("challenge")
    val challenge: String?
) : AuthRequestItem()

@Serializable
data class AuthUsePersonaRequestItem(
    @SerialName("identityAddress")
    val identityAddress: String
) : AuthRequestItem()

@Serializable(with = AuthRequestItemSerializer::class)
sealed class AuthRequestItem

fun AuthLoginRequestItem.toDomainModel(): MessageFromDataChannel.AuthRequest.LoginRequest {
    return MessageFromDataChannel.AuthRequest.LoginRequest(challenge.orEmpty())
}

fun AuthUsePersonaRequestItem.toDomainModel(): MessageFromDataChannel.AuthRequest.UsePersonaRequest {
    return MessageFromDataChannel.AuthRequest.UsePersonaRequest(identityAddress)
}

class AuthRequestItemSerializer : JsonContentPolymorphicSerializer<AuthRequestItem>(AuthRequestItem::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out AuthRequestItem> {
        return if (element.jsonObject["challenge"] != null) {
            AuthLoginRequestItem.serializer()
        } else {
            AuthUsePersonaRequestItem.serializer()
        }
    }
}
