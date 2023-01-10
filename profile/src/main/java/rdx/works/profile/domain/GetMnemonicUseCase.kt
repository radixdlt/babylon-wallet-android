package rdx.works.profile.domain

import com.radixdlt.bip39.generateMnemonic
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.datastore.EncryptedDataStore
import rdx.works.profile.data.model.factorsources.FactorSources.Companion.factorSourceId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

// TODO provide encryption as this is sensitive
class GetMnemonicUseCase @Inject constructor(
    private val encryptedDataStore: EncryptedDataStore,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    /**
     * We might have multiple OnDevice-HD-FactorSources, thus multiple mnemonics stored on the device.
     */
    private suspend fun readMnemonic(key: String): String =
        encryptedDataStore.getString("mnemonic$key").first().orEmpty()

    /**
     * We save mnemonic under specific key which will be factorSourceId
     */
    private suspend fun saveMnemonic(
        key: String,
        mnemonic: String
    ) = encryptedDataStore.putString("mnemonic$key", mnemonic)

    /**
     * Key is empty by default when no profile has been generated before
     * If profile exists we should pass factorSourceId here
     */
    suspend operator fun invoke(mnemonicKey: String? = null): String {
        /*
         * If key is not null, it means we have had factorSourceId and we can read existing mnemonic
         * Otherwise, we generate mnemonic, and calculate the key for it (factorSourceId)
         */
        mnemonicKey?.let { key ->
            return readMnemonic(key)
        } ?: run {
            return withContext(defaultDispatcher) {
                val mnemonic = generateMnemonic(
                    strength = ENTROPY_STRENGTH,
                    wordList = WORDLIST_ENGLISH
                )

                val key = factorSourceId(
                    mnemonic = MnemonicWords(
                        phrase = mnemonic
                    )
                )
                saveMnemonic(
                    key = key,
                    mnemonic = mnemonic
                )
                mnemonic
            }
        }
    }

    companion object {
        /**
         * This will tell you how random your entropy is, the more the better it has to be 128-256 bit, multiple of 32
         */
        const val ENTROPY_STRENGTH = 256
    }
}
