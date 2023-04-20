package rdx.works.profile.data.model

import com.radixdlt.bip39.generateMnemonic
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.model.ExtendedKey
import com.radixdlt.slip10.toKey
import kotlinx.serialization.Serializable
import rdx.works.core.toHexString
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.toEllipticCurveType
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.olympiaimport.OlympiaAccountDetails

@Serializable
data class MnemonicWithPassphrase(
    val mnemonic: String,
    val bip39Passphrase: String
)

/**
 * Generates a mnemonic based on the [WORDLIST_ENGLISH]. Used only when no mnemonic
 * exists.
 */
fun MnemonicWithPassphrase.Companion.generate(
    entropyStrength: Int
): MnemonicWithPassphrase = MnemonicWithPassphrase(
    mnemonic = generateMnemonic(
        strength = entropyStrength,
        wordList = WORDLIST_ENGLISH
    ),
    bip39Passphrase = ""
)

fun MnemonicWithPassphrase.compressedPublicKey(
    curve: Slip10Curve = Slip10Curve.CURVE_25519,
    derivationPath: DerivationPath,
): ByteArray {
    val words = MnemonicWords(mnemonic)
    val seed = words.toSeed(passphrase = bip39Passphrase)

    val derivedKey = seed.toKey(derivationPath.path, curve.toEllipticCurveType())

    return derivedKey.keyPair.getCompressedPublicKey()
}

@Suppress("UnsafeCallOnNullableType")
fun MnemonicWithPassphrase.deriveExtendedKey(
    factorInstance: FactorInstance
): ExtendedKey {
    val mnemonic = MnemonicWords(phrase = mnemonic)
    val seed = mnemonic.toSeed(passphrase = bip39Passphrase)

    return seed.toKey(
        factorInstance.derivationPath!!.path,
        factorInstance.publicKey.curve.toEllipticCurveType()
    )
}

fun MnemonicWithPassphrase.validatePublicKeysOf(accounts: List<OlympiaAccountDetails>): Boolean {
    val words = MnemonicWords(mnemonic)
    val seed = words.toSeed(passphrase = bip39Passphrase)
    accounts.forEach { account ->
        val derivedPublicKey =
            seed.toKey(account.derivationPath.path, EllipticCurveType.Secp256k1).keyPair.getCompressedPublicKey().toHexString()
        if (derivedPublicKey != account.publicKey) {
            return false
        }
    }
    return true
}
