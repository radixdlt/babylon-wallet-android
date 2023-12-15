package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.EncryptedProfileSnapshot
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.ProfileSerializer
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import javax.inject.Inject

interface BackupProfileRepository {

    suspend fun saveTemporaryRestoringSnapshot(snapshotSerialised: String, backupType: BackupType): Result<Unit>

    suspend fun getTemporaryRestoringProfile(backupType: BackupType): Profile?

    suspend fun discardTemporaryRestoringSnapshot(backupType: BackupType)

    suspend fun getSnapshotForBackup(backupType: BackupType): String?
}

class BackupProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager,
    @ProfileSerializer private val profileSnapshotJson: Json,
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
                val encryptedProfileSnapshot = runCatching {
                    profileSnapshotJson.decodeFromString<EncryptedProfileSnapshot>(snapshotSerialised)
                }.getOrNull()

                if (encryptedProfileSnapshot != null) {
                    Result.failure(ProfileException.InvalidPassword)
                } else {
                    Result.failure(ProfileException.InvalidSnapshot)
                }
            }
        }

        is BackupType.File.Encrypted -> {
            val encryptedProfileSnapshot = runCatching {
                profileSnapshotJson.decodeFromString<EncryptedProfileSnapshot>(snapshotSerialised)
            }.getOrNull()

            if (encryptedProfileSnapshot == null) {
                Result.failure(ProfileException.InvalidSnapshot)
            } else {
                val snapshot = runCatching {
                    val decrypted = encryptedProfileSnapshot.decrypt(
                        deserializer = profileSnapshotJson,
                        password = backupType.password
                    )
                    profileSnapshotJson.encodeToString(decrypted)
                }.getOrNull()

                if (snapshot != null) {
                    encryptedPreferencesManager.putProfileSnapshotFromFileBackup(snapshot)
                    Result.success(Unit)
                } else {
                    Result.failure(ProfileException.InvalidPassword)
                }
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
        if (profile == null || profile.mainBabylonFactorSource() == null) return null
        val snapshotSerialised = runCatching {
            profileSnapshotJson.encodeToString(profile.snapshot())
        }.getOrNull() ?: return null

        return when (backupType) {
            is BackupType.Cloud -> if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
                snapshotSerialised
            } else {
                null
            }

            is BackupType.File.PlainText -> snapshotSerialised
            is BackupType.File.Encrypted -> {
                val encryptedSnapshot = EncryptedProfileSnapshot.from(snapshotSerialised, backupType.password)
                runCatching { profileSnapshotJson.encodeToString(encryptedSnapshot) }.getOrNull()
            }
        }
    }
}
