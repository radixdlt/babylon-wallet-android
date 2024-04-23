package rdx.works.profile.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.KeySpec
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.logNonFatalException
import rdx.works.profile.di.ProfileDataStore
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ProfileDataStore private val preferences: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val encryptedProfile = preferences.data.retryWhen { cause, attempt ->
        // It seem that some users are experiencing IOException which results in empty preferences returned.
        // We retry to read preferences if IOException occurred, before we go to catchIOException() handler
        // Until we have crash reporting in place, we can try this.
        if (cause is IOException && attempt < RETRY_COUNT) {
            delay(RETRY_DELAY)
            true
        } else {
            false
        }
    }.catchIOException().map { preferences ->
        val preferencesKey = stringPreferencesKey(PROFILE_PREFERENCES_KEY)
        val encryptedValue = preferences[preferencesKey]
        if (encryptedValue.isNullOrEmpty()) {
            return@map null
        }
        encryptedValue.decrypt(KeySpec.Profile())
    }.flowOn(ioDispatcher)

    suspend fun readMnemonic(key: String): Result<String> {
        return preferences.data.catchIOException().map { preferences ->
            val preferencesKey = stringPreferencesKey(key)
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                return@map Result.failure(ProfileException.NoMnemonic)
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

    suspend fun getProfileSnapshotFromCloudBackup() = preferences.data.catchIOException().map { preferences ->
        val snapshotEncrypted = preferences[stringPreferencesKey(RESTORED_PROFILE_CLOUD_PREFERENCES_KEY)]

        if (snapshotEncrypted.isNullOrEmpty()) {
            null
        } else {
            snapshotEncrypted.decrypt(KeySpec.Profile())
        }
    }.flowOn(ioDispatcher).firstOrNull()?.getOrNull()

    suspend fun putProfileSnapshotFromCloudBackup(restoredSnapshotSerialized: String) {
        putString(RESTORED_PROFILE_CLOUD_PREFERENCES_KEY, restoredSnapshotSerialized, KeySpec.Profile())
    }

    suspend fun clearProfileSnapshotFromCloudBackup() {
        preferences.edit {
            it.remove(stringPreferencesKey(RESTORED_PROFILE_CLOUD_PREFERENCES_KEY))
        }
    }

    suspend fun getProfileSnapshotFromFileBackup() = preferences.data.catchIOException().map { preferences ->
        val snapshotEncrypted = preferences[stringPreferencesKey(RESTORED_PROFILE_FILE_PREFERENCES_KEY)]

        if (snapshotEncrypted.isNullOrEmpty()) {
            null
        } else {
            snapshotEncrypted.decrypt(KeySpec.Profile())
        }
    }.flowOn(ioDispatcher).firstOrNull()?.getOrNull()

    suspend fun putProfileSnapshotFromFileBackup(restoredSnapshotSerialized: String) {
        putString(RESTORED_PROFILE_FILE_PREFERENCES_KEY, restoredSnapshotSerialized, KeySpec.Profile())
    }

    suspend fun clearProfileSnapshotFromFileBackup() {
        preferences.edit {
            it.remove(stringPreferencesKey(RESTORED_PROFILE_FILE_PREFERENCES_KEY))
        }
    }

    suspend fun saveConnectorExtensionLinkPublicKeyPair(cePublicKey: String, privateKey: ByteArray) {
        val newKeyPairs = getP2PLinksKeys().toMutableMap()
            .apply { this[cePublicKey] = privateKey }
        saveP2PLinksKeys(newKeyPairs)
    }

    suspend fun getP2PLinkPrivateKey(cePublicKey: String): ByteArray? {
        return getP2PLinksKeys()[cePublicKey]
    }

    suspend fun removeP2PLinkKeys(cePublicKey: String) {
        val newKeyPairs = getP2PLinksKeys().toMutableMap()
            .apply { remove(cePublicKey) }
        saveP2PLinksKeys(newKeyPairs)
    }

    private suspend fun saveP2PLinksKeys(keyPairs: Map<String, ByteArray>) {
        val encryptedSerializedKeyPairs = Json.encodeToString(keyPairs)
        putString(P2P_LINKS_KEYS_PREFERENCES_KEY, encryptedSerializedKeyPairs, KeySpec.Profile())
    }

    private suspend fun getP2PLinksKeys(): Map<String, ByteArray> {
        return preferences.data.catchIOException()
            .map { preferences ->
                preferences[stringPreferencesKey(P2P_LINKS_KEYS_PREFERENCES_KEY)]
                    .takeIf { !it.isNullOrEmpty() }
                    ?.decrypt(KeySpec.Profile())
                    ?.getOrNull()
                    ?.let { Json.decodeFromString<Map<String, ByteArray>>(it) }
            }
            .flowOn(ioDispatcher)
            .firstOrNull()
            ?: emptyMap()
    }

    suspend fun clear() = preferences.edit { it.clear() }

    private fun Flow<Preferences>.catchIOException() = catch { exception ->
        if (exception is IOException) {
            logNonFatalException(exception)
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    companion object {
        const val DATA_STORE_NAME = "rdx_encrypted_datastore"
        private const val PROFILE_PREFERENCES_KEY = "profile_preferences_key"
        private const val RESTORED_PROFILE_CLOUD_PREFERENCES_KEY = "restored_cloud_profile_key"
        private const val RESTORED_PROFILE_FILE_PREFERENCES_KEY = "restored_file_profile_key"
        private const val P2P_LINKS_KEYS_PREFERENCES_KEY = "p2p_links_keys_key"
        private const val RETRY_COUNT = 3L
        private const val RETRY_DELAY = 1500L
    }
}
