package rdx.works.core

import java.security.KeyStore
import javax.inject.Inject

class KeystoreManager @Inject constructor() {

    fun removeKeys(): Result<Unit> {
        return runCatching {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS_MNEMONIC)
            keyStore.deleteEntry(KEY_ALIAS_PROFILE)
        }
    }

    fun removeMnemonicEncryptionKey(): Result<Unit> {
        return runCatching {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS_MNEMONIC)
        }
    }

    companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS_PROFILE = "EncryptedProfileAlias"
        const val KEY_ALIAS_MNEMONIC = "EncryptedMnemonicAlias"
    }
}
