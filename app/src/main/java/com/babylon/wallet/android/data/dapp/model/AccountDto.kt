package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    @SerialName("address")
    val address: String,
    @SerialName("label")
    val label: String,
    @SerialName("appearanceId")
    val appearanceId: Int
)

@Serializable
data class AccountWithProofOfOwnership(
    @SerialName("account")
    val account: Account,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("signature")
    val signature: String
)

@Serializable
data class PersonaDataField(
    @SerialName("field")
    val field: String,
    @SerialName("value")
    val value: String
)
