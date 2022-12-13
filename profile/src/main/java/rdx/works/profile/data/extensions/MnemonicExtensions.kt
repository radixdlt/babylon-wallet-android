package rdx.works.profile.data.extensions

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.model.PrivateKey
import com.radixdlt.slip10.toKey
import rdx.works.profile.data.utils.hashToFactorId

fun MnemonicWords.compressedPublicKey(
    ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
    derivationPath: String,
    bip39Passphrase: String = "",
): ByteArray {
    val seed = toSeed(passphrase = bip39Passphrase)

    val derivedKey = seed.toKey(derivationPath, ellipticCurveType)

    return derivedKey.keyPair.getCompressedPublicKey()
}

fun MnemonicWords.signerPrivateKey(
    factorSourceId: String,
    ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
    derivationPath: String,
    bip39Passphrase: String = "",
): PrivateKey {
    val seed = toSeed(passphrase = bip39Passphrase)
    val derivedKey = seed.toKey(derivationPath, ellipticCurveType)
    val sourceId = derivedKey.keyPair.getCompressedPublicKey().hashToFactorId()
    if (sourceId != factorSourceId) throw Exception()
    return derivedKey.keyPair.privateKey
}
