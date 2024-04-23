package rdx.works.profile.data.repository

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.serializedJsonString
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.InstantGenerator
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.encryptWithPassword
import rdx.works.core.sargon.init
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import javax.inject.Inject

// TODO integration
interface BackupProfileRepository {

    suspend fun saveTemporaryRestoringSnapshot(snapshotSerialised: String, backupType: BackupType): Result<Unit>

    suspend fun getTemporaryRestoringProfile(backupType: BackupType): Profile?

    suspend fun discardTemporaryRestoringSnapshot(backupType: BackupType)

    suspend fun getSnapshotForBackup(backupType: BackupType): String?
}

class BackupProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository
) : BackupProfileRepository {

    override suspend fun saveTemporaryRestoringSnapshot(
        snapshotSerialised: String,
        backupType: BackupType
    ): Result<Unit> = when (backupType) {
        is BackupType.Cloud -> {
            if (profileRepository.deriveProfileState(snapshotSerialised) is ProfileState.Restored) {
                encryptedPreferencesManager.putProfileSnapshotFromCloudBackup(snapshotSerialised)
                preferencesManager.updateLastBackupInstant(InstantGenerator())
                Result.success(Unit)
            } else {
                Result.failure(ProfileException.InvalidSnapshot)
            }
        }

        is BackupType.File.PlainText -> {
            val profileState = profileRepository.deriveProfileState(snapshotSerialised)
            if (profileState is ProfileState.Restored) {
                encryptedPreferencesManager.putProfileSnapshotFromFileBackup(snapshotSerialised)
                Result.success(Unit)
            } else {
                Result.failure(ProfileException.InvalidSnapshot)
            }
        }

        is BackupType.File.Encrypted -> {
            val profile = runCatching {
                Profile.init(encrypted = snapshotSerialised, password = backupType.password)
            }.getOrNull()

            if (profile == null) {
                Result.failure(ProfileException.InvalidPassword)
            } else {
                val snapshot = profile.serializedJsonString()
                encryptedPreferencesManager.putProfileSnapshotFromFileBackup(snapshot)
                Result.success(Unit)
            }
        }
    }

    override suspend fun getTemporaryRestoringProfile(backupType: BackupType): Profile? = when (backupType) {
        is BackupType.Cloud -> encryptedPreferencesManager.getProfileSnapshotFromCloudBackup()
        is BackupType.File -> encryptedPreferencesManager.getProfileSnapshotFromFileBackup()
    }?.let { snapshot ->
        profileRepository.deriveProfileState(snapshot) as? ProfileState.Restored
    }?.profile

    override suspend fun discardTemporaryRestoringSnapshot(backupType: BackupType) = when (backupType) {
        is BackupType.Cloud -> {
            encryptedPreferencesManager.clearProfileSnapshotFromCloudBackup()
            preferencesManager.removeLastBackupInstant()
        }
        is BackupType.File -> {
            encryptedPreferencesManager.clearProfileSnapshotFromFileBackup()
        }
    }

    /**
     * we only back up fully initialized profile, that has Babylon Factor Source
     */
    override suspend fun getSnapshotForBackup(backupType: BackupType): String? {
        val profile = profileRepository.profile.firstOrNull()
        if (profile == null || profile.mainBabylonFactorSource == null) return null
        return when (backupType) {
            is BackupType.Cloud -> if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
                profile.serializedJsonString()
            } else {
                null
            }
            is BackupType.File.PlainText -> profile.serializedJsonString()
            is BackupType.File.Encrypted -> profile.encryptWithPassword(backupType.password)
        }
    }
}
