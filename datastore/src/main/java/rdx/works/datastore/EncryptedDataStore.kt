package rdx.works.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class EncryptedDataStore(
    private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATA_STORE_NAME
    )

    fun getString(key: String): Flow<String?> {
        val preferencesKey = stringPreferencesKey(key)
        return context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                return@map null
            }
            decryptData(Base64.decode(encryptedValue, Base64.DEFAULT))
        }
    }

    suspend fun putString(key: String, newValue: String?) {
        val preferencesKey = stringPreferencesKey(key)
        newValue?.let { newValueNotNull ->
            val encryptedValue = Base64.encodeToString(encryptData(newValueNotNull.toByteArray()), Base64.DEFAULT)
            context.dataStore.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "rdx_encrypted_datastore"
    }
}
