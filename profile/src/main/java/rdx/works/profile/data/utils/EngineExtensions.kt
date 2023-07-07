@file:Suppress("TooGenericExceptionThrown")

package rdx.works.profile.data.utils

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.model.PrivateKey
import rdx.works.core.toUByteList
import com.radixdlt.ret.PublicKey as EnginePublicKey
import rdx.works.core.crypto.PrivateKey as EnginePrivateKey

fun ECKeyPair.toEnginePublicKeyModel(): EnginePublicKey {
    return when (this.publicKey.curveType) {
        EllipticCurveType.Secp256k1 -> {
            // Required size 33 bytes
            EnginePublicKey.EcdsaSecp256k1(getCompressedPublicKey().toUByteList()) // TODO RET
        }
        EllipticCurveType.Ed25519 -> {
            // Required size 32 bytes
            EnginePublicKey.EddsaEd25519(getCompressedPublicKey().removeLeadingZero().toUByteList())
        }
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}

fun PrivateKey.toEngineModel(): EnginePrivateKey {
    return when (this.curveType) {
        EllipticCurveType.Secp256k1 -> EnginePrivateKey.EcdsaSecp256k1.newFromPrivateKeyBytes(
            this.keyByteArray()
        )
        EllipticCurveType.Ed25519 -> EnginePrivateKey.EddsaEd25519.newFromPrivateKeyBytes(this.keyByteArray())
        EllipticCurveType.P256 -> throw Exception("Curve EllipticCurveType.P256 not supported")
    }
}
