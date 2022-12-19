package com.babylon.wallet.android.domain.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.model.PrivateKey

fun ECKeyPair.toEnginePublicKeyModel(): models.crypto.PublicKey {
    return when (this.publicKey.curveType) {
        EllipticCurveType.Secp256k1 -> models.crypto.PublicKey.EcdsaSecp256k1.fromByteArray(getCompressedPublicKey().removeLeadingZero())
        EllipticCurveType.Ed25519 -> models.crypto.PublicKey.EddsaEd25519.fromByteArray(getCompressedPublicKey().removeLeadingZero())
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}

fun PrivateKey.toEngineModel(): models.crypto.PrivateKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> models.crypto.PrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(this.key.toByteArray())
        EllipticCurveType.Ed25519 -> models.crypto.PrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(this.key.toByteArray())
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}