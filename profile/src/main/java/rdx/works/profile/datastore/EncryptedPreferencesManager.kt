package rdx.works.profile.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.profile.di.ProfileDataStore
import rdx.works.profile.di.coroutines.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ProfileDataStore private val preferences: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val encryptedProfile = preferences.data.catchIOException().map { preferences ->
        val preferencesKey = stringPreferencesKey(PROFILE_PREFERENCES_KEY)
        val encryptedValue = preferences[preferencesKey]
        if (encryptedValue.isNullOrEmpty()) {
            return@map null
        }
        encryptedValue.decrypt(KEY_ALIAS_DATASTORE)
    }.flowOn(ioDispatcher)

    suspend fun readMnemonic(key: String): String? {
        return preferences.data.catchIOException().map { preferences ->
            val preferencesKey = stringPreferencesKey("mnemonic$key")
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                return@map null
            }
            encryptedValue.decrypt(KEY_ALIAS_DATASTORE)
        }.first()
    }

    suspend fun putString(key: String, newValue: String?) {
        val preferencesKey = stringPreferencesKey(key)
        newValue?.let { newValueNotNull ->
            val encryptedValue = newValueNotNull.encrypt(withKeyAlias = KEY_ALIAS_DATASTORE)
            preferences.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
        }
    }

    suspend fun putProfileSnapshot(snapshotSerialized: String) {
        putString(PROFILE_PREFERENCES_KEY, snapshotSerialized)
    }

    suspend fun getProfileSnapshotFromBackup() = preferences.data.catchIOException().map { preferences ->
        val snapshotEncrypted = preferences[stringPreferencesKey(RESTORED_PROFILE_PREFERENCES_KEY)]

        if (snapshotEncrypted.isNullOrEmpty()) {
            null
        } else {
            snapshotEncrypted.decrypt(KEY_ALIAS_DATASTORE)
        }
    }.firstOrNull()

    suspend fun putProfileSnapshotFromBackup(restoredSnapshotSerialized: String) {
        putString(RESTORED_PROFILE_PREFERENCES_KEY, restoredSnapshotSerialized)
    }

    suspend fun clear() = preferences.edit { it.clear() }

    private fun Flow<Preferences>.catchIOException() = catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }


    companion object {
        const val DATA_STORE_NAME = "rdx_encrypted_datastore"
        private const val PROFILE_PREFERENCES_KEY = "profile_preferences_key"
        private const val RESTORED_PROFILE_PREFERENCES_KEY = "restored_preferences_key"
        private const val KEY_ALIAS_DATASTORE = "EncryptedDataStoreAlias"
    }
}
