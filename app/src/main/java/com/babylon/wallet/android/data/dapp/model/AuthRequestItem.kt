package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.HexCoded32Bytes

@Serializable
sealed class AuthRequestItem

@Serializable
sealed class AuthLoginRequestItem : AuthRequestItem()

@Serializable
@SerialName("loginWithChallenge")
data class AuthLoginWithChallengeRequestItem(
    @SerialName("challenge")
    val challenge: String? = null
) : AuthLoginRequestItem()

@Serializable
@SerialName("loginWithoutChallenge")
data object AuthLoginWithoutChallengeRequestItem : AuthLoginRequestItem()

@Serializable
@SerialName("usePersona")
data class AuthUsePersonaRequestItem(
    @SerialName("identityAddress")
    val identityAddress: String
) : AuthRequestItem()

fun AuthLoginRequestItem.toDomainModel(): AuthorizedRequest.AuthRequest.LoginRequest {
    return when (this) {
        is AuthLoginWithChallengeRequestItem -> AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge(
            challenge = HexCoded32Bytes(
                challenge.orEmpty()
            )
        )

        is AuthLoginWithoutChallengeRequestItem -> AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge
    }
}

fun AuthUsePersonaRequestItem.toDomainModel(): AuthorizedRequest.AuthRequest.UsePersonaRequest {
    return AuthorizedRequest.AuthRequest.UsePersonaRequest(identityAddress)
}
