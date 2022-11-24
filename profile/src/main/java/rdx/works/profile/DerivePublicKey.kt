package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.slip10.toKey

interface DerivePublicKey {
    fun derive(derivationPath: String): ByteArray
}

class CompressedPublicKey(
    private val mnemonic: MnemonicWords
) : DerivePublicKey {

    override fun derive(derivationPath: String): ByteArray {
        val seed = mnemonic.toSeed()

        val derivedKey = seed.toKey(derivationPath, EllipticCurveType.Ed25519)

        return derivedKey.keyPair.getCompressedPublicKey()
    }
}