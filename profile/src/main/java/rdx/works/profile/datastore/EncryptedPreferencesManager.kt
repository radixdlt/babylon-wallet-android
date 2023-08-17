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
import rdx.works.core.KeySpec
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
        encryptedValue.decrypt(KeySpec.Profile())
    }.flowOn(ioDispatcher)

    suspend fun readMnemonic(key: String): Result<String>? {
        return preferences.data.catchIOException().map { preferences ->
            val preferencesKey = stringPreferencesKey("mnemonic$key")
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                return@map null
            }
            val result = encryptedValue.decrypt(KeySpec.Mnemonic())
            result
        }.first()
    }

    suspend fun saveMnemonic(key: String, newValue: String?) {
        putString(key, newValue, KeySpec.Mnemonic())
    }

    suspend fun keyExist(key: String): Boolean {
        val preferencesKey = stringPreferencesKey(key)
        return preferences.data.map { preference ->
            preference.contains(preferencesKey)
        }.first()
    }

    private suspend fun putString(key: String, newValue: String?, keySpec: KeySpec) {
        val preferencesKey = stringPreferencesKey(key)
        newValue?.let { newValueNotNull ->
            val encryptedValue = newValueNotNull.encrypt(withKey = keySpec).getOrThrow()
            preferences.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
        }
    }

    suspend fun removeEntryForKey(key: String) {
        preferences.edit {
            it.remove(stringPreferencesKey(key))
        }
    }

    suspend fun putProfileSnapshot(snapshotSerialized: String) {
        putString(PROFILE_PREFERENCES_KEY, snapshotSerialized, KeySpec.Profile())
    }

    suspend fun getProfileSnapshotFromBackup() = preferences.data.catchIOException().map { preferences ->
        val snapshotEncrypted = preferences[stringPreferencesKey(RESTORED_PROFILE_PREFERENCES_KEY)]

        if (snapshotEncrypted.isNullOrEmpty()) {
            null
        } else {
            snapshotEncrypted.decrypt(KeySpec.Profile())
        }
    }.flowOn(ioDispatcher).firstOrNull()

    suspend fun putProfileSnapshotFromBackup(restoredSnapshotSerialized: String) {
        putString(RESTORED_PROFILE_PREFERENCES_KEY, restoredSnapshotSerialized, KeySpec.Profile())
    }

    suspend fun clearProfileSnapshotFromBackup() {
        preferences.edit {
            it.remove(stringPreferencesKey(RESTORED_PROFILE_PREFERENCES_KEY))
        }
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
    }
}
