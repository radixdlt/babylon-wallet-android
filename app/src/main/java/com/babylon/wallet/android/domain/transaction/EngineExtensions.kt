package com.babylon.wallet.android.domain.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.model.PrivateKey
import com.radixdlt.model.PublicKey

fun PublicKey.toEngineModel(): models.crypto.PublicKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> models.crypto.PublicKey.EcdsaSecp256k1.fromByteArray(this.key.toByteArray())
        EllipticCurveType.Ed25519 -> models.crypto.PublicKey.EcdsaSecp256k1.fromByteArray(this.key.toByteArray())
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