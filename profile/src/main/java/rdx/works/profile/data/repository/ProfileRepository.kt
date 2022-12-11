package rdx.works.profile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

// TODO will have to add encryption
interface ProfileRepository {
    suspend fun saveProfileSnapshot(profileSnapshot: ProfileSnapshot)
    suspend fun readProfileSnapshot(): ProfileSnapshot?
}

class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ProfileRepository {

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
        private val PROFILE_PREFERENCES_KEY = stringPreferencesKey("profile")
    }
}
