package com.babylon.wallet.android.data.dapp.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.ret.crypto.SignatureWithPublicKey

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

fun SignatureWithPublicKey.toProof(signedMessage: ByteArray): Proof = when (this) {
    is SignatureWithPublicKey.Secp256k1 -> Proof(
        publicKey = publicKeyHex(signedMessage),
        signature = signatureHex,
        curve = Proof.Curve.Secp256k1
    )
    is SignatureWithPublicKey.Ed25519 -> Proof(
        publicKey = publicKeyHex,
        signature = signatureHex,
        curve = Proof.Curve.Curve25519
    )
}
