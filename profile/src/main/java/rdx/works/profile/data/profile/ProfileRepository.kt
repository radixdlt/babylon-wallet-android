package rdx.works.profile.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.Profile
import javax.inject.Inject

//TODO will have to add encryption
interface ProfileRepository {
    suspend fun saveProfile(profile: Profile)
    suspend fun readProfile(): Profile?
}

class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ProfileRepository {

    override suspend fun saveProfile(profile: Profile) {
        val profileContent = Json.encodeToString(profile)
        dataStore.edit { preferences ->
            preferences[PROFILE_PREFERENCES_KEY] = profileContent
        }
    }

    override suspend fun readProfile(): Profile? {
        val profileContent = dataStore.data
            .map { preferences ->
                preferences[PROFILE_PREFERENCES_KEY] ?: ""
            }.first()
        return if (profileContent.isEmpty()) {
            null
        } else {
            Json.decodeFromString(profileContent)
        }
    }

    companion object {
        private val PROFILE_PREFERENCES_KEY = stringPreferencesKey("profile")
    }
}
