package rdx.works.profile.ret.crypto

import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.model.ECKeyPair
import com.radixdlt.ret.Address
import com.radixdlt.ret.OlympiaNetwork
import com.radixdlt.ret.deriveOlympiaAccountAddressFromPublicKey
import com.radixdlt.ret.deriveVirtualAccountAddressFromPublicKey
import com.radixdlt.ret.deriveVirtualIdentityAddressFromPublicKey

private typealias EnginePublicKeyEd25519 = com.radixdlt.ret.PublicKey.Ed25519
private typealias EnginePublicKeySecp256k1 = com.radixdlt.ret.PublicKey.Secp256k1

sealed interface PublicKey {


    class Ed25519(
        value: ByteArray
    ): PublicKey {

        private val engineKey = EnginePublicKeyEd25519(value)

        val value: ByteArray
            get() = engineKey.value

        fun deriveAccountAddress(networkId: Int): String {
            return deriveVirtualAccountAddressFromPublicKey(publicKey = engineKey, networkId = networkId.toUByte()).addressString()
        }

        fun deriveIdentityAddress(networkId: Int): String {
            return deriveVirtualIdentityAddressFromPublicKey(publicKey = engineKey, networkId = networkId.toUByte()).addressString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Ed25519

            return engineKey == other.engineKey
        }

        override fun hashCode(): Int {
            return engineKey.hashCode()
        }

    }

    class Secp256k1(
        value: ByteArray
    ): PublicKey {

        private val engineKey = EnginePublicKeySecp256k1(value)

        fun deriveOlympiaAccountAddress(networkId: Int): String {
            val olympiaAddress = deriveOlympiaAccountAddressFromPublicKey(publicKey = engineKey, olympiaNetwork = OlympiaNetwork.MAINNET)
            return Address.virtualAccountAddressFromOlympiaAddress(
                olympiaAccountAddress = olympiaAddress,
                networkId = networkId.toUByte()
            ).addressString()
        }

        val value: ByteArray
            get() = engineKey.value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Secp256k1

            return engineKey == other.engineKey
        }

        override fun hashCode(): Int {
            return engineKey.hashCode()
        }


    }

    companion object {
        fun ECKeyPair.toPublicKey(): PublicKey {
            return when (publicKey.curveType) {
                EllipticCurveType.Secp256k1 -> {
                    // Required size 33 bytes
                    Secp256k1(getCompressedPublicKey())
                }
                EllipticCurveType.Ed25519 -> {
                    // Required size 32 bytes
                    Ed25519(getCompressedPublicKey().removeLeadingZero())
                }
                EllipticCurveType.P256 -> error("Curve EllipticCurveType.P256 not supported")
            }
        }
    }
}