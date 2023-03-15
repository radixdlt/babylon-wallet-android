package rdx.works.profile.data.extensions

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.model.ExtendedKey
import com.radixdlt.slip10.toKey
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance

fun MnemonicWords.compressedPublicKey(
    ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
    derivationPath: String,
    bip39Passphrase: String = "",
): ByteArray {
    val seed = toSeed(passphrase = bip39Passphrase)

    val derivedKey = seed.toKey(derivationPath, ellipticCurveType)

    return derivedKey.keyPair.getCompressedPublicKey()
}

fun MnemonicWords.deriveExtendedKey(
    factorInstance: FactorInstance,
    bip39Passphrase: String
): ExtendedKey {
    val seed = toSeed(passphrase = bip39Passphrase)

    val ellipticCurveType = when (factorInstance.publicKey.curve) {
        Slip10Curve.CURVE_25519 -> EllipticCurveType.Ed25519
        Slip10Curve.SECP_256K1 -> EllipticCurveType.Secp256k1
    }

    return seed.toKey(factorInstance.derivationPath!!.path, ellipticCurveType)
}
