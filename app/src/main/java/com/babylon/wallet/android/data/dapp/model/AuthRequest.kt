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

fun AuthLoginRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest {
    return MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.LoginRequest(challenge.orEmpty())
}

fun AuthUsePersonaRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest {
    return MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(identityAddress)
}
