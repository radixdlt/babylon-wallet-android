package rdx.works.profile.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import rdx.works.core.decryptData
import rdx.works.core.encrypt
import rdx.works.profile.di.ProfileDataStore
import rdx.works.profile.di.coroutines.IoDispatcher
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ProfileDataStore private val preferences: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val encryptedProfile = preferences.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val preferencesKey = stringPreferencesKey(PROFILE_PREFERENCES_KEY)
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                return@map null
            }
            val decryptedBytes = Base64.decode(encryptedValue, Base64.DEFAULT)
                .decryptData(KEY_ALIAS_DATASTORE)
            String(decryptedBytes, StandardCharsets.UTF_8)
        }
        .flowOn(ioDispatcher)

    suspend fun readMnemonic(key: String): String? {
        return preferences.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                val preferencesKey = stringPreferencesKey("mnemonic$key")
                val encryptedValue = preferences[preferencesKey]
                if (encryptedValue.isNullOrEmpty()) {
                    return@map null
                }
                val decryptedBytes = Base64.decode(encryptedValue, Base64.DEFAULT)
                    .decryptData(KEY_ALIAS_DATASTORE)
                String(decryptedBytes, StandardCharsets.UTF_8)
            }.first()
    }

    suspend fun putString(key: String, newValue: String?) {
        val preferencesKey = stringPreferencesKey(key)
        newValue?.let { newValueNotNull ->
            val encryptedValue = Base64.encodeToString(
                newValueNotNull.toByteArray().encrypt(withKeyAlias = KEY_ALIAS_DATASTORE),
                Base64.DEFAULT
            )
            preferences.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
        }
    }

    fun getBytes(key: String): Flow<ByteArray?> {
        val preferencesKey = stringPreferencesKey(key)
        return preferences.data.catch { exception ->
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
            Base64.decode(encryptedValue, Base64.DEFAULT).decryptData(KEY_ALIAS_DATASTORE)
        }
    }

    suspend fun putProfileBytes(newValue: ByteArray?) {
        val preferencesKey = stringPreferencesKey(PROFILE_PREFERENCES_KEY)
        newValue?.let { newValueNotNull ->
            val encryptedValue = Base64.encodeToString(
                newValueNotNull.encrypt(KEY_ALIAS_DATASTORE),
                Base64.DEFAULT
            )
            preferences.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
        }
    }

    suspend fun clear() = preferences.edit { it.clear() }

    companion object {
        const val DATA_STORE_NAME = "rdx_encrypted_datastore"
        private const val PROFILE_PREFERENCES_KEY = "profile_preferences_key"
        private const val KEY_ALIAS_DATASTORE = "EncryptedDataStoreAlias"
    }
}
