package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginWithoutChallengeResponseItem(
    override val requestType: String,
    @SerialName("personaId")
    val personaId: String
) : WalletResponseItem()

@Serializable
data class LoginWithChallengeResponseItem(
    override val requestType: String,
    @SerialName("personaId")
    val personaId: String,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("publicKey")
    val publicKey: String,
    @SerialName("signature")
    val signature: String,
    @SerialName("identityComponentAddress")
    val identityComponentAddress: String
) : WalletResponseItem()

enum class LoginRequestType(val requestType: String) {
    LOGIN_READ("loginRead")
}
