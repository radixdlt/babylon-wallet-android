package com.babylon.wallet.android.data.dapp.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.ret.crypto.SignatureWithPublicKey
import rdx.works.profile.ret.publicKey
import rdx.works.profile.ret.signature
import rdx.works.profile.ret.toHexString

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

fun com.radixdlt.ret.SignatureWithPublicKey.toProof(signedMessage: ByteArray): Proof {
    return when (val signatureWithPublicKey = this) {
        is com.radixdlt.ret.SignatureWithPublicKey.Secp256k1 -> Proof(
            signatureWithPublicKey.publicKey(signedMessage).toHexString(),
            signatureWithPublicKey.signature().toHexString(),
            Proof.Curve.Secp256k1
        )

        is com.radixdlt.ret.SignatureWithPublicKey.Ed25519 -> Proof(
            signatureWithPublicKey.publicKey().toHexString(),
            signatureWithPublicKey.signature().toHexString(),
            Proof.Curve.Curve25519
        )
    }
}
