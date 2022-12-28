@file:Suppress("TooGenericExceptionThrown")

package com.babylon.wallet.android.domain.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.model.PrivateKey

fun ECKeyPair.toEnginePublicKeyModel(): com.radixdlt.toolkit.models.crypto.PublicKey {
    return when (this.publicKey.curveType) {
        EllipticCurveType.Secp256k1 -> {
            com.radixdlt.toolkit.models.crypto.PublicKey.EcdsaSecp256k1.fromByteArray(getCompressedPublicKey().removeLeadingZero())
        }
        EllipticCurveType.Ed25519 -> {
            com.radixdlt.toolkit.models.crypto.PublicKey.EddsaEd25519.fromByteArray(getCompressedPublicKey())
        }
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}

fun PrivateKey.toEngineModel(): com.radixdlt.toolkit.models.crypto.PrivateKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> {
            com.radixdlt.toolkit.models.crypto.PrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(this.key.toByteArray())
        }
        EllipticCurveType.Ed25519 -> com.radixdlt.toolkit.models.crypto.PrivateKey.EddsaEd25519.newFromPrivateKeyBytes(
            this.key.toByteArray())
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}
