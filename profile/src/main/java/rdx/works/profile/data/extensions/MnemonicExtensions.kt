package rdx.works.profile.data.extensions

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.toKey
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.derivation.CustomHDDerivationPath

/**
 * This generates the key, in our case factorSourceID, which we can use to associate with mnemonics when we store it
 * For now defaults to Ed25519 curve and getId custom derivation path
 */
fun MnemonicWords.factorSourceId(
    ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
    derivationPath: String = CustomHDDerivationPath.getId.path,
    bip39Passphrase: String = ""
): String {
    return compressedPublicKey(
        ellipticCurveType = ellipticCurveType,
        derivationPath = derivationPath,
        bip39Passphrase = bip39Passphrase
    ).hashToFactorId()
}

fun MnemonicWords.compressedPublicKey(
    ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
    derivationPath: String,
    bip39Passphrase: String = "",
): ByteArray {
    val seed = toSeed(passphrase = bip39Passphrase)

    val derivedKey = seed.toKey(derivationPath, ellipticCurveType)

    return derivedKey.keyPair.getCompressedPublicKey()
}
