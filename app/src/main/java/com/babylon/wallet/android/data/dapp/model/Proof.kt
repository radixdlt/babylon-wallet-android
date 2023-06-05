package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.decodeHex

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

fun SignatureWithPublicKey.toProof(challengeHex: String): Proof {
    return when (val signatureWithPublicKey = this) {
        is SignatureWithPublicKey.EcdsaSecp256k1 -> Proof(
            signatureWithPublicKey.publicKey(challengeHex.decodeHex()).toString(),
            signatureWithPublicKey.signature().toString(),
            Proof.Curve.Secp256k1
        )
        is SignatureWithPublicKey.EddsaEd25519 -> Proof(
            signatureWithPublicKey.publicKey().toString(),
            signatureWithPublicKey.signature().toString(),
            Proof.Curve.Curve25519
        )
    }
}
