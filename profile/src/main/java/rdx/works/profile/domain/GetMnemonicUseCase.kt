package rdx.works.profile.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radixdlt.bip39.generateMnemonic
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rdx.works.profile.domain.GetMnemonicUseCase.Companion.ENTROPY_STRENGTH
import javax.inject.Inject

//TODO provide encryption as this is sensitive
class GetMnemonicUseCase @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private suspend fun readMnemonic(): String = dataStore.data
        .map { preferences ->
            preferences[MNEMONIC] ?: ""
        }.first()

    private suspend fun saveMnemonic(mnemonic: String) {
        dataStore.edit { preferences ->
            preferences[MNEMONIC] = mnemonic
        }
    }

    suspend operator fun invoke(): MnemonicWords {
        // Read mnemonic from local storage first
        var deviceFactorSourceMnemonic = readMnemonic()

        // If empty, it means it was never generated, so create new one
        if (deviceFactorSourceMnemonic.isEmpty()) {
            deviceFactorSourceMnemonic = generateMnemonic().toString()
            saveMnemonic(deviceFactorSourceMnemonic)
        }
        return MnemonicWords(
            phrase = deviceFactorSourceMnemonic
        )
    }

    companion object {
        private val MNEMONIC = stringPreferencesKey("mnemonic")
        /**
         * This will tell you how random your entropy is, the more the better it has to be 128-256 bit, multiple of 32
         */
        const val ENTROPY_STRENGTH = 256
    }
}

private fun generateMnemonic(): MnemonicWords {
    return MnemonicWords(
        phrase = generateMnemonic(
            strength = ENTROPY_STRENGTH,
            wordList = WORDLIST_ENGLISH
        )
    )
}
