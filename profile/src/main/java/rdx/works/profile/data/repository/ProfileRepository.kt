package rdx.works.profile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.di.coroutines.DefaultDispatcher
import java.io.IOException
import javax.inject.Inject

// TODO will have to add encryption
interface ProfileRepository {

    suspend fun saveProfileSnapshot(profileSnapshot: ProfileSnapshot)

    suspend fun readProfileSnapshot(): ProfileSnapshot?

    val p2pClient: Flow<P2PClient?>
}

class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override val p2pClient: Flow<P2PClient?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val profileJsonString = preferences[PROFILE_PREFERENCES_KEY] ?: ""
            val profileSnapshot = Json.decodeFromString<ProfileSnapshot>(profileJsonString)
            profileSnapshot.appPreferences.p2pClients.firstOrNull()
        }

    override suspend fun saveProfileSnapshot(profileSnapshot: ProfileSnapshot) {
        withContext(defaultDispatcher) {
            val profileContent = Json.encodeToString(profileSnapshot)
            dataStore.edit { preferences ->
                preferences[PROFILE_PREFERENCES_KEY] = profileContent
            }
        }
    }

    override suspend fun readProfileSnapshot(): ProfileSnapshot? {
        return withContext(defaultDispatcher) {
            val profileContent = dataStore.data
                .map { preferences ->
                    preferences[PROFILE_PREFERENCES_KEY] ?: ""
                }.first()
            if (profileContent.isEmpty()) {
                null
            } else {
                Json.decodeFromString<ProfileSnapshot>(profileContent)
            }
        }
    }

    companion object {
        private val PROFILE_PREFERENCES_KEY = stringPreferencesKey("profile_preferences_key")
    }
}
