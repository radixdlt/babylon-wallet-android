package rdx.works.core

import com.radixdlt.sargon.os.storage.KeySpec
import javax.inject.Inject

class KeystoreManager @Inject constructor() {

    fun removeKeys(): Result<Unit> = KeySpec.remove(
        listOf(
            KeySpec.Profile(),
            KeySpec.Mnemonic()
        )
    )

    fun regenerateMnemonicEncryptionKey(): Result<Unit> {
        return KeySpec.Mnemonic().generateSecretKey().toUnitResult()
    }
}
