package rdx.works.profile.data.repository

import com.radixdlt.sargon.Bip39Language
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class MnemonicRepository @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    /**
     * We might have multiple OnDevice-HD-FactorSources, thus multiple mnemonics stored on the device.
     */
    @Suppress("SwallowedException")
    suspend fun readMnemonic(key: FactorSourceId.Hash): Result<MnemonicWithPassphrase> {
        return encryptedPreferencesManager.readMnemonic("mnemonic${key.value.body.hex}").mapCatching {
            MnemonicWithPassphrase.fromJson(it)
        }
    }

    @Suppress("SwallowedException")
    suspend fun mnemonicExist(key: FactorSourceId.Hash): Boolean {
        return encryptedPreferencesManager.keyExist("mnemonic${key.value.body.hex}")
    }

    /**
     * We save mnemonic under specific key which will be factorSourceId
     */
    suspend fun saveMnemonic(
        key: FactorSourceId.Hash,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val serialised = mnemonicWithPassphrase.toJson()
        return encryptedPreferencesManager.saveMnemonic("mnemonic${key.value.body.hex}", serialised)
    }

    suspend fun deleteMnemonic(
        key: FactorSourceId.Hash
    ) {
        encryptedPreferencesManager.removeEntryForKey("mnemonic${key.value.body.hex}")
    }

    /**
     * Used to return or generate a new mnemonic. The mnemonic can:
     * 1. Not exist in the first place or we didn't pass a key:
     *    In this case we generate a new mnemonic, and based on that a "default" factor source id.`
     * 2. Exist, but could not be deserialized properly:
     *    This should not happen to the end users, but as we refactor the project we used to save
     *    only the mnemonic words. Now we save a json representation of both the mnemonic words and
     *    the passphrase. In such a scenario, when the user upgrades to the newest version, we will
     *    not be able to deserialize properly. In this case we generate a new mnemonic like in (1).
     * 3. We passed a key and the mnemonic exists:
     *    We deserialize it properly and just return that back.
     */
    suspend operator fun invoke(mnemonicKey: FactorSourceId.Hash? = null): Result<MnemonicWithPassphrase> {
        return mnemonicKey?.let { readMnemonic(key = it) } ?: withContext(defaultDispatcher) {
            val generated =   MnemonicWithPassphrase(
                mnemonic = Mnemonic.init(
                    wordCount = Bip39WordCount.TWENTY_FOUR,
                    language = Bip39Language.ENGLISH
                ),
                passphrase = ""
            )

            val key = FactorSourceId.Hash.init(kind = FactorSourceKind.DEVICE, mnemonicWithPassphrase = it)
            saveMnemonic(key = key, mnemonicWithPassphrase = generated).mapCatching { generated }
        }
    }
}
