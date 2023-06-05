package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountProof(
    @SerialName("accountAddress") val accountAddress: String,
    @SerialName("proof") val proof: Proof
)
