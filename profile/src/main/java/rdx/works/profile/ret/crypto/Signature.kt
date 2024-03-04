package rdx.works.profile.ret.crypto

private typealias EngineSignatureEd25519 = com.radixdlt.ret.Signature.Ed25519
private typealias EngineSignatureSecp256k1 = com.radixdlt.ret.Signature.Secp256k1

sealed interface Signature {

    class Ed25519(
        value: ByteArray
    ) : Signature {

        private val signature = EngineSignatureEd25519(value)

        val value: ByteArray
            get() = signature.value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Ed25519

            return signature == other.signature
        }

        override fun hashCode(): Int {
            return signature.hashCode()
        }
    }

    class Secp256k1(
        value: ByteArray
    ) : Signature {

        private val signature = EngineSignatureSecp256k1(value)

        val value: ByteArray
            get() = signature.value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Secp256k1

            return signature == other.signature
        }

        override fun hashCode(): Int {
            return signature.hashCode()
        }
    }
}
