package rdx.works.core

import com.radixdlt.sargon.os.storage.KeySpec
import javax.inject.Inject

class KeystoreManager @Inject constructor() {

    fun resetKeySpecs(): Result<Unit> = KeySpec.reset(
        listOf(
            KeySpec.Profile(),
            KeySpec.Mnemonic()
        )
    )

    fun resetMnemonicKeySpec(): Result<Unit> = KeySpec.reset(listOf(KeySpec.Mnemonic()))

}
