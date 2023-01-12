@file:Suppress("TooGenericExceptionThrown")

package com.babylon.wallet.android.data.transaction

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.model.PrivateKey
import com.radixdlt.toolkit.models.crypto.PrivateKey as EnginePrivateKey
import com.radixdlt.toolkit.models.crypto.PublicKey as EnginePublicKey

fun ECKeyPair.toEnginePublicKeyModel(): EnginePublicKey {
    return when (this.publicKey.curveType) {
        EllipticCurveType.Secp256k1 -> {
            // Required size 33 bytes
            EnginePublicKey.EcdsaSecp256k1.fromByteArray(getCompressedPublicKey())
        }
        EllipticCurveType.Ed25519 -> {
            // Required size 32 bytes
            EnginePublicKey.EddsaEd25519.fromByteArray(getCompressedPublicKey().removeLeadingZero())
        }
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}

fun PrivateKey.toEngineModel(): EnginePrivateKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> EnginePrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(this.keyByteArray())
        EllipticCurveType.Ed25519 -> EnginePrivateKey.EddsaEd25519.newFromPrivateKeyBytes(this.keyByteArray())
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}
