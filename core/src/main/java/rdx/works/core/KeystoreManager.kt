package rdx.works.core

import com.radixdlt.sargon.os.storage.KeySpec
import javax.inject.Inject

class KeystoreManager @Inject constructor() {

    fun removeKeys(): Result<Unit> = KeySpec.Mnemonic().delete().then {
        KeySpec.Profile().delete()
    }

    fun removeMnemonicEncryptionKey(): Result<Unit> = KeySpec.Mnemonic().delete()
}
