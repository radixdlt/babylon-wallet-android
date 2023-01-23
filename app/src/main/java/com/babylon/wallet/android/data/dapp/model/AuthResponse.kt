package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("auth1")
data class AuthLoginWithoutChallengeRequestResponseItem(
    @SerialName("personaId")
    val personaId: String
) : AuthLoginRequestResponseItem()

@Serializable
@SerialName("auth2")
data class AuthLoginWithChallengeRequestResponseItem(
    @SerialName("persona")
    val persona: PersonaDto,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("publicKey")
    val publicKey: String,
    @SerialName("signature")
    val signature: String
) : AuthLoginRequestResponseItem()

@Serializable
sealed class AuthLoginRequestResponseItem : AuthRequestResponseItem()

@Serializable
sealed class AuthRequestResponseItem

@Serializable
data class AuthUsePersonaRequestResponseItem(
    @SerialName("identityAddress")
    val identityAddress: String
) : AuthRequestResponseItem()
