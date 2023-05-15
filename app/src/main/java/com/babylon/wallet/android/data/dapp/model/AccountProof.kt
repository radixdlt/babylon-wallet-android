package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountProof(
    @SerialName("accountAddress") val accountAddress: String,
    @SerialName("proof") val proof: Proof
) {
    @Serializable
    data class Proof(
        val publicKey: String,
        val signature: String,
        val curve: Curve
    ) {
        @Serializable
        enum class Curve {
            @SerialName("curve25519")
            Curve25519,

            @SerialName("secp256k1")
            Secp256k1
        }
    }
}
