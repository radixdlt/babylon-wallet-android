package com.babylon.wallet.android.data.ce.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@SerialName("loginWithoutChallenge")
data class AuthLoginWithoutChallengeRequestResponseItem(
    @SerialName("persona")
    val persona: PersonaDto
) : AuthLoginRequestResponseItem()

@Serializable
@SerialName("loginWithChallenge")
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class AuthLoginRequestResponseItem : AuthRequestResponseItem()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class AuthRequestResponseItem

@Serializable
@SerialName("usePersona")
data class AuthUsePersonaRequestResponseItem(
    @SerialName("persona")
    val persona: PersonaDto
) : AuthRequestResponseItem()
