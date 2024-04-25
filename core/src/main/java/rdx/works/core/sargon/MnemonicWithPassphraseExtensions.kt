package rdx.works.core.sargon

import com.radixdlt.bip39.generateMnemonic
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import com.radixdlt.sargon.BIP39Passphrase
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.slip10.model.ExtendedKey
import com.radixdlt.slip10.toKey
import rdx.works.core.crypto.PrivateKey
import rdx.works.core.crypto.PrivateKey.Companion.toPrivateKey
import rdx.works.core.hash

fun MnemonicWithPassphrase.Companion.init(phrase: String) = MnemonicWithPassphrase(
    mnemonic = Mnemonic.init(phrase = phrase),
    passphrase = BIP39Passphrase()
)

/**
 * Generates a mnemonic based on the [WORDLIST_ENGLISH]. Used only when no mnemonic
 * exists.
 */
fun MnemonicWithPassphrase.Companion.generate(
    entropyStrength: Int
): MnemonicWithPassphrase = MnemonicWithPassphrase(
    mnemonic = Mnemonic.init(
        phrase = generateMnemonic(
            strength = entropyStrength,
            wordList = WORDLIST_ENGLISH
        )
    ),
    passphrase = BIP39Passphrase()
)

fun MnemonicWithPassphrase.toFactorSourceId(
    curve: Slip10Curve = Slip10Curve.CURVE25519
): FactorSourceId.Hash = FactorSourceIdFromHash(
    kind = FactorSourceKind.DEVICE,
    body = toExtendedKey(
        curve = curve,
        derivationPath = DerivationPath.getIdPath()
    ).keyPair.getCompressedPublicKey().removeLeadingZero().hash().bytes
).asGeneral()

fun MnemonicWithPassphrase.derivePrivateKey(
    hdPublicKey: HierarchicalDeterministicPublicKey
): PrivateKey = toExtendedKey(
    curve = when (hdPublicKey.publicKey) {
        is PublicKey.Ed25519 -> Slip10Curve.CURVE25519
        is PublicKey.Secp256k1 -> Slip10Curve.SECP256K1
    },
    derivationPath = hdPublicKey.derivationPath
).keyPair.toPrivateKey()

fun MnemonicWithPassphrase.derivePublicKey(
    derivationPath: DerivationPath,
    curve: Slip10Curve = Slip10Curve.CURVE25519
): PublicKey {
    val publicKeyBytes = toExtendedKey(curve = curve, derivationPath = derivationPath)
        .keyPair
        .getCompressedPublicKey()
        .removeLeadingZero()
        .toBagOfBytes()

    return PublicKey.init(bytes = publicKeyBytes)
}

private fun MnemonicWithPassphrase.toExtendedKey(
    curve: Slip10Curve = Slip10Curve.CURVE25519,
    derivationPath: DerivationPath,
): ExtendedKey {
    val words = MnemonicWords(mnemonic.words.map { it.word })
    val seed = words.toSeed(passphrase = passphrase)

    return seed.toKey(derivationPath.string, curve.toEllipticCurveType())
}

fun MnemonicWithPassphrase.validateAgainst(factorSource: FactorSource.Device): Boolean =
    toFactorSourceId().value.body == factorSource.value.id.body

fun Bip39WordCount.Companion.init(discriminant: UByte) =
    Bip39WordCount.entries.find { it.value == discriminant } ?: error("Not valid Bip39WordCount value of `$discriminant`")

private fun Slip10Curve.toEllipticCurveType() = when (this) {
    Slip10Curve.CURVE25519 -> EllipticCurveType.Ed25519
    Slip10Curve.SECP256K1 -> EllipticCurveType.Secp256k1
}
