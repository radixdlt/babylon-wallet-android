package rdx.works.profile.datastore

import android.security.keystore.UserNotAuthenticatedException
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import rdx.works.core.KeySpec
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.logNonFatalException
import rdx.works.profile.di.PermanentDataStore
import rdx.works.profile.di.ProfileDataStore
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ProfileDataStore private val preferences: DataStore<Preferences>,
    @PermanentDataStore private val permanentPreferences: DataStore<Preferences>,
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

    val preferencesSize = preferences.data.catchIOException().map { preferences ->
        preferences.asMap().size
    }.flowOn(ioDispatcher).distinctUntilChanged()

    private val p2pLinksKeySpec by lazy { KeySpec.Cache(P2P_LINKS_CACHE_KEY_ALIAS) }

    suspend fun readMnemonic(key: String): Result<String> {
        return preferences.data.catchIOException().map { preferences ->
            val preferencesKey = stringPreferencesKey(key)
            val encryptedValue = preferences[preferencesKey]
            if (encryptedValue.isNullOrEmpty()) {
                Result.failure(ProfileException.NoMnemonic)
            } else {
                encryptedValue.decrypt(KeySpec.Mnemonic())
            }
        }.first().fold(onSuccess = { Result.success(it) }, onFailure = {
            if (it is UserNotAuthenticatedException) {
                Result.failure(ProfileException.SecureStorageAccess)
            } else {
                Result.failure(it)
            }
        })
    }

    suspend fun saveMnemonic(key: String, newValue: String): Result<Unit> {
        return putString(key, newValue, KeySpec.Mnemonic()).fold(onSuccess = {
            Result.success(Unit)
        }, onFailure = {
            if (it is UserNotAuthenticatedException) {
                Result.failure(ProfileException.SecureStorageAccess)
            } else {
                Result.failure(it)
            }
        })
    }

    suspend fun keyExist(key: String): Boolean {
        val preferencesKey = stringPreferencesKey(key)
        return preferences.data.map { preference ->
            preference.contains(preferencesKey)
        }.first()
    }

    private suspend fun putString(key: String, newValue: String, keySpec: KeySpec): Result<Unit> {
        val preferencesKey = stringPreferencesKey(key)
        return newValue.encrypt(withKey = keySpec).mapCatching { encryptedValue ->
            preferences.edit { mutablePreferences ->
                mutablePreferences[preferencesKey] = encryptedValue
            }
            Unit
        }
    }

    private suspend fun putString(prefs: DataStore<Preferences>, key: String, newValue: String?, keySpec: KeySpec) {
        val preferencesKey = stringPreferencesKey(key)
        newValue?.let { newValueNotNull ->
            val encryptedValue = newValueNotNull.encrypt(withKey = keySpec).getOrThrow()
            prefs.edit { mutablePreferences ->
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

    fun getP2PLinkListJson(): Flow<Result<String>?> {
        return permanentPreferences.data.catchIOException()
            .map { preferences ->
                preferences[stringPreferencesKey(P2P_LINKS_PREFERENCES_KEY)]
                    .takeIf { !it.isNullOrEmpty() }
                    ?.decrypt(p2pLinksKeySpec)
            }
    }

    suspend fun saveP2PLinkListJson(p2pLinkListJson: String) {
        putString(permanentPreferences, P2P_LINKS_PREFERENCES_KEY, p2pLinkListJson, p2pLinksKeySpec)
    }

    suspend fun saveP2PLinksWalletPrivateKey(privateKeyHex: String) {
        putString(permanentPreferences, P2P_LINKS_WALLET_PK_PREFERENCES_KEY, privateKeyHex, p2pLinksKeySpec)
    }

    suspend fun getP2PLinksWalletPrivateKey(): String? {
        return permanentPreferences.data.catchIOException()
            .map { preferences ->
                preferences[stringPreferencesKey(P2P_LINKS_WALLET_PK_PREFERENCES_KEY)]
                    .takeIf { !it.isNullOrEmpty() }
                    ?.decrypt(p2pLinksKeySpec)
            }
            .firstOrNull()
            ?.getOrNull()
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
        const val PERMANENT_DATA_STORE_NAME = "rdx_permanent_datastore"
        private const val PROFILE_PREFERENCES_KEY = "profile_preferences_key"
        private const val RESTORED_PROFILE_CLOUD_PREFERENCES_KEY = "restored_cloud_profile_key"
        private const val RESTORED_PROFILE_FILE_PREFERENCES_KEY = "restored_file_profile_key"
        private const val P2P_LINKS_PREFERENCES_KEY = "p2p_links_key"
        private const val P2P_LINKS_WALLET_PK_PREFERENCES_KEY = "p2p_links_wallet_pk_key"
        private const val P2P_LINKS_CACHE_KEY_ALIAS = "p2pLinksCacheKey"
        private const val RETRY_COUNT = 3L
        private const val RETRY_DELAY = 1500L
    }
}
