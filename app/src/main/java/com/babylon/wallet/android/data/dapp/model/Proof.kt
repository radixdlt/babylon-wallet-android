package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.SignatureWithPublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.decodeHex
import rdx.works.core.toByteArray
import rdx.works.core.toHexString
import rdx.works.core.toUByteList

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

// TODO RET
fun SignatureWithPublicKey.toProof(challengeHex: String): Proof {
    return when (val signatureWithPublicKey = this) {
        is SignatureWithPublicKey.EcdsaSecp256k1 -> Proof(
            PublicKey.EcdsaSecp256k1(challengeHex.decodeHex().toUByteList()).value.toByteArray().toHexString(),
            signatureWithPublicKey.signature.toString(),
            Proof.Curve.Secp256k1
        )
        is SignatureWithPublicKey.EddsaEd25519 -> Proof(
            PublicKey.EddsaEd25519(signatureWithPublicKey.publicKey).value.toByteArray().toHexString(),
            signatureWithPublicKey.signature.toString(),
            Proof.Curve.Curve25519
        )
    }
}
