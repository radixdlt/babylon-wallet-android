package rdx.works.profile.data.repository

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.checkIfProfileJsonContainsLegacyP2PLinks
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson
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
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.ApplicationScope
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

interface ProfileRepository {

    val profileState: Flow<ProfileState>

    val inMemoryProfileOrNull: Profile?

    suspend fun saveProfile(profile: Profile)

    suspend fun clearProfileDataOnly()

    suspend fun clearAllWalletData()

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
    private val cloudBackupSyncExecutor: CloudBackupSyncExecutor,
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
                ensureP2PLinkMigrationAcknowledged(snapshot)
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
            header = profile.header.copy(lastModified = TimestampGenerator())
        )
        withContext(ioDispatcher) {
            val profileContent = profileToSave.toJson()
            // Store profile
            encryptedPreferencesManager.putProfileSnapshot(profileContent)

            if (profileToSave.canBackupToCloud) {
                cloudBackupSyncExecutor.requestCloudBackup()
            } else {
                encryptedPreferencesManager.clearProfileSnapshotFromCloudBackup()
            }

            // Update the flow and notify Backup Manager that it needs to backup
            profileStateFlow.update { ProfileState.Restored(profileToSave) }
        }
    }

    override suspend fun clearAllWalletData() {
        preferencesManager.clear()
        clearProfileDataOnly()
    }

    override suspend fun clearProfileDataOnly() {
        encryptedPreferencesManager.clear()
        profileStateFlow.update { ProfileState.None }
    }

    @Suppress("SwallowedException")
    override fun deriveProfileState(content: String): ProfileState = runCatching {
        Profile.fromJson(content)
    }.fold(
        onSuccess = {
            ProfileState.Restored(it)
        },
        onFailure = {
            Timber.w(it)
            ProfileState.Incompatible
        }
    )

    private suspend fun ensureP2PLinkMigrationAcknowledged(profileJson: String) {
        val isP2PLinkMigrationCheckPerformed = preferencesManager.showRelinkConnectorsAfterUpdate.firstOrNull() != null
        if (isP2PLinkMigrationCheckPerformed) {
            return
        }

        preferencesManager.setShowRelinkConnectorsAfterUpdate(
            show = Profile.checkIfProfileJsonContainsLegacyP2PLinks(profileJson)
        )
    }
}
