package rdx.works.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.generate
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
    private suspend fun readMnemonic(key: FactorSource.ID): MnemonicWithPassphrase? {
        val serialised = encryptedPreferencesManager.readMnemonic(key.value).orEmpty()
        return try {
            Json.decodeFromString(serialised)
        } catch (exception: Exception) {
            return null
        }
    }

    /**
     * We save mnemonic under specific key which will be factorSourceId
     */
    private suspend fun saveMnemonic(
        key: FactorSource.ID,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ) {
        val serialised = Json.encodeToString(mnemonicWithPassphrase)
        encryptedPreferencesManager.putString("mnemonic${key.value}", serialised)
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
    suspend operator fun invoke(mnemonicKey: FactorSource.ID? = null): MnemonicWithPassphrase {
        return mnemonicKey?.let { readMnemonic(key = it) } ?: withContext(defaultDispatcher) {
            val generated = MnemonicWithPassphrase.generate(entropyStrength = ENTROPY_STRENGTH)

            val key = FactorSource.factorSourceId(mnemonicWithPassphrase = generated)
            saveMnemonic(key = key, mnemonicWithPassphrase = generated)
            generated
        }
    }

    companion object {
        /**
         * This will tell you how random your entropy is, the more the better it has to be 128-256 bit, multiple of 32
         */
        const val ENTROPY_STRENGTH = 256
    }
}
