package rdx.works.profile.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.crypto.Crypto
import javax.inject.Inject

//TODO provide encryption as this is sensitive
class GenerateMnemonicUseCase @Inject constructor(
    private val crypto: Crypto,
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
            deviceFactorSourceMnemonic = crypto.generateMnemonic().toString()
            saveMnemonic(deviceFactorSourceMnemonic)
        }
        return MnemonicWords(
            phrase = deviceFactorSourceMnemonic
        )
    }

    companion object {
        private val MNEMONIC = stringPreferencesKey("mnemonic")
    }
}