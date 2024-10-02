package rdx.works.core

import com.radixdlt.sargon.os.storage.KeySpec
import javax.inject.Inject

class KeystoreManager @Inject constructor() {

    /**
     * The user, at any time, can reset their device PIN/PATTERN, thus making the mnemonic master key useless. The reason is that the
     * mnemonic is tied to the device's biometrics. In order to be able to encrypt the mnemonic again, with the same master key alias,
     * we need to delete the old entry (or generate a new one in versions <31).
     *
     * This method deletes (or regenerates in older android versions) the master key **only** when is deemed useless.
     * Check the [KeySpec.Mnemonic.checkIfPermanentlyInvalidated] method for more info.
     *
     * This method should be called when:
     * - The user is about to create a new wallet
     * - restore a wallet from backup
     * - derive a wallet from a seed phrase
     * - whenever a profile exists, and the app comes to the foreground. The previous cases, so far require no profile. In those cases, we
     * cannot just reset the mnemonic key spec on the fly, due to the complex nature of multiple mnemonics entries. On the other hand
     * when a profile exists, we can reset the master key, delete the mnemonics, and notify the user that recovery is required.
     * - deleting a wallet
     */
    fun resetMnemonicKeySpecWhenInvalidated(): Result<Boolean> = if (KeySpec.Mnemonic().checkIfPermanentlyInvalidated()) {
        KeySpec.reset(listOf(KeySpec.Mnemonic())).map { true }
    } else {
        Result.success(false)
    }
}
