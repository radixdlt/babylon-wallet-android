package rdx.works.core.sargon

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.cap26PathToString
import com.radixdlt.sargon.defaultGetIdPath
import com.radixdlt.slip10.model.ExtendedKey
import com.radixdlt.slip10.toKey
import rdx.works.core.hash

fun MnemonicWithPassphrase.toFactorSourceId(
    curve: Slip10Curve = Slip10Curve.CURVE25519
): Exactly32Bytes = toExtendedKey(
    curve = curve,
    derivationPath = DerivationPath.Cap26(Cap26Path.GetId(defaultGetIdPath())) // TODO extension
).keyPair.getCompressedPublicKey().removeLeadingZero().hash()

fun MnemonicWithPassphrase.toExtendedKey(
    curve: Slip10Curve = Slip10Curve.CURVE25519,
    derivationPath: DerivationPath,
): ExtendedKey {
    val words = MnemonicWords(mnemonic.words.map { it.word })
    val seed = words.toSeed(passphrase = passphrase)

    val pathString = when (derivationPath) {
        is DerivationPath.Bip44Like -> ""
        is DerivationPath.Cap26 -> cap26PathToString(derivationPath.value) // TODO extension
    }

    return seed.toKey(pathString, curve.toEllipticCurveType())
}

private fun Slip10Curve.toEllipticCurveType() = when (this) {
    Slip10Curve.CURVE25519 -> EllipticCurveType.Ed25519
    Slip10Curve.SECP256K1 -> EllipticCurveType.Secp256k1
}