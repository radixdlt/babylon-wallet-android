package rdx.works.profile.data.repository

import android.app.backup.BackupManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.ProfileSnapshotRelaxed
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.ProfileSerializer
import rdx.works.profile.di.RelaxedSerializer
import rdx.works.profile.di.coroutines.ApplicationScope
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface ProfileRepository {

    val profileState: Flow<ProfileState>

    val inMemoryProfileOrNull: Profile?

    suspend fun saveProfile(profile: Profile)

    suspend fun clear()

    fun deriveProfileState(content: String): ProfileState
}

suspend fun ProfileRepository.updateProfile(updateAction: suspend (Profile) -> Profile): Profile {
    val profile = profile.first()
    val updatedProfile = updateAction(profile)
    saveProfile(updatedProfile)
    return updatedProfile
}

val ProfileRepository.profile: Flow<Profile>
    get() = profileState
        .filterIsInstance<ProfileState.Restored>()
        .map { it.profile }

@Suppress("LongParameterList")
class ProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager,
    @RelaxedSerializer private val relaxedJson: Json,
    private val backupManager: BackupManager,
    @ProfileSerializer private val profileSnapshotJson: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope
) : ProfileRepository {

    init {
        applicationScope.launch {
            val snapshotResult = encryptedPreferencesManager.encryptedProfile.firstOrNull()

            if (snapshotResult == null) {
                profileStateFlow.update { ProfileState.None }
            } else {
                val snapshot = snapshotResult.getOrNull().orEmpty()
                profileStateFlow.update { deriveProfileState(snapshot) }
            }
        }
    }

    private val profileStateFlow: MutableStateFlow<ProfileState> = MutableStateFlow(ProfileState.NotInitialised)

    override val profileState = profileStateFlow
        .filterNot { it is ProfileState.NotInitialised }

    override val inMemoryProfileOrNull: Profile?
        get() = when (val state = profileStateFlow.value) {
            is ProfileState.Restored -> state.profile
            else -> null
        }

    override suspend fun saveProfile(profile: Profile) {
        val profileToSave = profile.copy(
            header = profile.header.copy(lastModified = InstantGenerator())
        )
        withContext(ioDispatcher) {
            val profileContent = profileSnapshotJson.encodeToString(profileToSave.snapshot())
            // Store profile
            encryptedPreferencesManager.putProfileSnapshot(profileContent)

            // Update the flow and notify Backup Manager that it needs to backup
            profileStateFlow.update { ProfileState.Restored(profileToSave) }

            backupManager.dataChanged()
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
        preferencesManager.clear()
        profileStateFlow.update { ProfileState.None }
        backupManager.dataChanged()
    }

    @Suppress("SwallowedException")
    override fun deriveProfileState(content: String): ProfileState {
        val snapshotRelaxed = try {
            relaxedJson.decodeFromString<ProfileSnapshotRelaxed>(content)
        } catch (exception: IllegalArgumentException) {
            return ProfileState.Incompatible
        }

        return if (snapshotRelaxed.isValid) {
            val snapshot = profileSnapshotJson.decodeFromString<ProfileSnapshot>(content)
            ProfileState.Restored(snapshot.toProfile())
        } else {
            ProfileState.Incompatible
        }
    }
}
