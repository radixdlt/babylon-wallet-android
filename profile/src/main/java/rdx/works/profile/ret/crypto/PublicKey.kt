package rdx.works.profile.ret.crypto

private typealias EnginePublicKeyEd25519 = com.radixdlt.ret.PublicKey.Ed25519
private typealias EnginePublicKeySecp256k1 = com.radixdlt.ret.PublicKey.Secp256k1

interface PublicKey {

    class Ed25519(
        value: ByteArray
    ): PublicKey {

        private val engineKey = EnginePublicKeyEd25519(value)

        val value: ByteArray
            get() = engineKey.value

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
    ) {

        private val engineKey = EnginePublicKeySecp256k1(value)

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

}