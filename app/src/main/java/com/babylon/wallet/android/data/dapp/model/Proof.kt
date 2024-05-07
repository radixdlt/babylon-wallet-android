package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.publicKey
import com.radixdlt.sargon.extensions.signature
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

fun SignatureWithPublicKey.toProof(): Proof = Proof(
    publicKey = this.publicKey.hex,
    signature = this.signature.string,
    curve = when (this) {
        is SignatureWithPublicKey.Ed25519 -> Proof.Curve.Curve25519
        is SignatureWithPublicKey.Secp256k1 -> Proof.Curve.Secp256k1
    }
)
