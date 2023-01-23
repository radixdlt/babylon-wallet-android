package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("login")
data class AuthLoginRequestItem(
    @SerialName("challenge")
    val challenge: String? = null
) : AuthRequestItem()

@Serializable
@SerialName("usePersona")
data class AuthUsePersonaRequestItem(
    @SerialName("identityAddress")
    val identityAddress: String
) : AuthRequestItem()

@Serializable
sealed class AuthRequestItem

fun AuthLoginRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.AuthRequest.LoginRequest {
    return MessageFromDataChannel.IncomingRequest.AuthRequest.LoginRequest(challenge.orEmpty())
}

fun AuthUsePersonaRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.AuthRequest.UsePersonaRequest {
    return MessageFromDataChannel.IncomingRequest.AuthRequest.UsePersonaRequest(identityAddress)
}
