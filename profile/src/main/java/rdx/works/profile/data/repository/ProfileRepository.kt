package rdx.works.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.ApplicationScope
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface ProfileRepository {

    val profileState: Flow<ProfileState>

    suspend fun saveProfile(profile: Profile)

    suspend fun clear()
}

val ProfileRepository.profile: Flow<Profile>
    get() = profileState
        .filter { it is ProfileState.Restored }
        .map { (it as ProfileState.Restored).profile }

class ProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val relaxedJson: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope
) : ProfileRepository {

    init {
        applicationScope.launch {
            val serialised = encryptedPreferencesManager.encryptedProfile.firstOrNull()

            if (serialised == null) {
                profileStateFlow.value = ProfileState.None
                return@launch
            }

            val profileIncompatible = relaxedJson.decodeFromString<ProfileSnapshot.ProfileVersionHolder>(
                serialised
            ).version < Profile.LATEST_PROFILE_VERSION

            if (profileIncompatible) {
                profileStateFlow.value = ProfileState.Incompatible
            } else {
                val snapshot = Json.decodeFromString<ProfileSnapshot>(serialised)
                profileStateFlow.value = ProfileState.Restored(snapshot.toProfile())
            }
        }
    }

    private val profileStateFlow: MutableStateFlow<ProfileState> = MutableStateFlow(ProfileState.NotInitialised)

    override val profileState = profileStateFlow
        .filterNot { it is ProfileState.NotInitialised }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileSnapshot(profileContent)
            profileStateFlow.value = ProfileState.Restored(profile)
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
        profileStateFlow.value = ProfileState.None
    }
}
