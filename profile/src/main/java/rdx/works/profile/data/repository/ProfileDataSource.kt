package rdx.works.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface ProfileDataSource {

    val profileState: Flow<ProfileState>

    val profile: Flow<Profile>

    suspend fun saveProfile(profile: Profile)

    suspend fun clear()
}

@Suppress("TooManyFunctions")
class ProfileDataSourceImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val relaxedJson: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileDataSource {

    @Suppress("SwallowedException")
    override val profileState: Flow<ProfileState> =
        encryptedPreferencesManager.encryptedProfile
            .map { profileContent ->
                profileContent?.let { profile ->
                    val profileVersion = relaxedJson.decodeFromString<ProfileSnapshot.ProfileVersionHolder>(profile)
                    val profileIncompatible = profileVersion.version < Profile.LATEST_PROFILE_VERSION
                    if (profileIncompatible) {
                        ProfileState.Incompatible
                    } else {
                        ProfileState.Restored(Json.decodeFromString<ProfileSnapshot>(profile).toProfile())
                    }
                } ?: ProfileState.None
            }

    override val profile: Flow<Profile> =
        profileState
            .filter { it is ProfileState.Restored }
            .map { (it as ProfileState.Restored).profile }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileSnapshot(profileContent)
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
    }
}
