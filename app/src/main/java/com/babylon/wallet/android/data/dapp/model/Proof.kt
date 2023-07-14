package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.ret.SignatureWithPublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.decodeHex
import rdx.works.core.ret.publicKey
import rdx.works.core.ret.signature
import rdx.works.core.ret.toHexString

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
            signatureWithPublicKey.publicKey(challengeHex.decodeHex()).toHexString(),
            signatureWithPublicKey.signature().toHexString(),
            Proof.Curve.Secp256k1
        )
        is SignatureWithPublicKey.EddsaEd25519 -> Proof(
            signatureWithPublicKey.publicKey().toHexString(),
            signatureWithPublicKey.signature().toHexString(),
            Proof.Curve.Curve25519
        )
    }
}
