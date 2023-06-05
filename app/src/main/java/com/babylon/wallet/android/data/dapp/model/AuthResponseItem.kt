package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class AuthRequestResponseItem

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class AuthLoginRequestResponseItem : AuthRequestResponseItem()

@Serializable
@SerialName("loginWithoutChallenge")
data class AuthLoginWithoutChallengeRequestResponseItem(
    @SerialName("persona")
    val persona: Persona
) : AuthLoginRequestResponseItem()

@Serializable
@SerialName("loginWithChallenge")
data class AuthLoginWithChallengeRequestResponseItem(
    @SerialName("persona")
    val persona: Persona,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("proof")
    val proof: Proof
) : AuthLoginRequestResponseItem()

@Serializable
@SerialName("usePersona")
data class AuthUsePersonaRequestResponseItem(
    @SerialName("persona")
    val persona: Persona
) : AuthRequestResponseItem()

@Serializable
data class Persona(
    @SerialName("identityAddress") val identityAddress: String,
    @SerialName("label") val label: String,
)
