package rdx.works.profile.data.repository

import android.app.backup.BackupManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    suspend fun isRestoredProfileFromBackupExists(): Boolean

    suspend fun getRestoredProfileFromBackup(): Profile?

    suspend fun clearBackedUpProfile()
}

val ProfileRepository.profile: Flow<Profile>
    get() = profileState
        .filter { it is ProfileState.Restored }
        .map { (it as ProfileState.Restored).profile }

class ProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val relaxedJson: Json,
    private val backupManager: BackupManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope
) : ProfileRepository {

    init {
        applicationScope.launch {
            val serialised = encryptedPreferencesManager.encryptedProfile.firstOrNull()

            if (serialised == null) {
                // No profile exists, checking for backup data to restore
                val profileStateFromBackup = encryptedPreferencesManager.getProfileSnapshotFromBackup()?.let {
                    deriveProfileState(it)
                }

                profileStateFlow.update { ProfileState.None(profileStateFromBackup is ProfileState.Restored) }
                return@launch
            }

            val profileState = deriveProfileState(serialised)
            profileStateFlow.update { profileState }
        }
    }

    private val profileStateFlow: MutableStateFlow<ProfileState> = MutableStateFlow(ProfileState.NotInitialised)

    override val profileState = profileStateFlow
        .filterNot { it is ProfileState.NotInitialised }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileSnapshot(profileContent)
            profileStateFlow.update { ProfileState.Restored(profile) }
            backupManager.dataChanged()
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
        profileStateFlow.update { ProfileState.None() }
        backupManager.dataChanged()
    }

    override suspend fun isRestoredProfileFromBackupExists(): Boolean {
        return getRestoredProfileFromBackup() != null
    }

    override suspend fun getRestoredProfileFromBackup(): Profile? {
        val state = encryptedPreferencesManager.getProfileSnapshotFromBackup()?.let { snapshot ->
            deriveProfileState(snapshot)
        }

        return (state as? ProfileState.Restored)?.profile
    }

    override suspend fun clearBackedUpProfile() {
        encryptedPreferencesManager.clearProfileSnapshotFromBackup()
    }

    private fun deriveProfileState(snapshotSerialised: String): ProfileState {
        val version = relaxedJson.decodeFromString<ProfileSnapshot.ProfileVersionHolder>(snapshotSerialised).version

        return if (version < Profile.LATEST_PROFILE_VERSION) {
            ProfileState.Incompatible
        } else {
            val snapshot = ProfileSnapshot.fromJson(snapshotSerialised)
            ProfileState.Restored(snapshot.toProfile())
        }
    }
}
