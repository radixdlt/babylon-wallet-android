package rdx.works.profile.data.repository

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileFileContents
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.extensions.analyzeContentsOfFile
import com.radixdlt.sargon.extensions.fromEncryptedJson
import com.radixdlt.sargon.extensions.toEncryptedJson
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import timber.log.Timber
import javax.inject.Inject

interface BackupProfileRepository {

    suspend fun saveTemporaryRestoringSnapshot(snapshotSerialised: String, backupType: BackupType): Result<Unit>

    suspend fun getTemporaryRestoringProfile(backupType: BackupType): Profile?

    suspend fun discardTemporaryRestoringSnapshot(backupType: BackupType)

    suspend fun getSnapshotForBackup(backupType: BackupType): String?
}

class BackupProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val profileRepository: ProfileRepository
) : BackupProfileRepository {

    override suspend fun saveTemporaryRestoringSnapshot(
        snapshotSerialised: String,
        backupType: BackupType
    ): Result<Unit> = when (backupType) {
        is BackupType.Cloud, BackupType.DeprecatedCloud -> {
            Timber.tag("CloudBackup").d("Save temporary restoring profile from $backupType")
            if (profileRepository.deriveProfileState(snapshotSerialised) is ProfileState.Loaded) {
                encryptedPreferencesManager.putProfileSnapshotFromCloudBackup(snapshotSerialised)
                Result.success(Unit)
            } else {
                Result.failure(ProfileException.InvalidSnapshot)
            }
        }

        is BackupType.File.PlainText -> {
            when (val contents = Profile.analyzeContentsOfFile(snapshotSerialised)) {
                is ProfileFileContents.EncryptedProfile -> Result.failure(ProfileException.InvalidPassword)
                is ProfileFileContents.PlaintextProfile -> {
                    encryptedPreferencesManager.putProfileSnapshotFromFileBackup(contents.v1.toJson())
                    Result.success(Unit)
                }
                is ProfileFileContents.NotProfile -> Result.failure(ProfileException.InvalidSnapshot)
            }
        }

        is BackupType.File.Encrypted -> {
            val contents = Profile.analyzeContentsOfFile(snapshotSerialised)
            when (contents) {
                is ProfileFileContents.EncryptedProfile -> {
                    runCatching {
                        Profile.fromEncryptedJson(jsonString = snapshotSerialised, decryptionPassword = backupType.password)
                    }.fold(
                        onSuccess = {
                            encryptedPreferencesManager.putProfileSnapshotFromFileBackup(it.toJson())
                            Result.success(Unit)
                        },
                        onFailure = {
                            Result.failure(ProfileException.InvalidPassword)
                        }
                    )
                }
                else -> Result.failure(ProfileException.InvalidSnapshot)
            }
        }
    }

    override suspend fun getTemporaryRestoringProfile(backupType: BackupType): Profile? = when (backupType) {
        is BackupType.DeprecatedCloud, is BackupType.Cloud -> {
            Timber.tag("CloudBackup").d("Get temporary restoring profile from $backupType")
            encryptedPreferencesManager.getProfileSnapshotFromCloudBackup()
        }
        is BackupType.File -> encryptedPreferencesManager.getProfileSnapshotFromFileBackup()
    }?.let { snapshot ->
        profileRepository.deriveProfileState(snapshot) as? ProfileState.Loaded
    }?.v1

    override suspend fun discardTemporaryRestoringSnapshot(backupType: BackupType) = when (backupType) {
        is BackupType.Cloud -> {
            // do nothing
        }
        is BackupType.DeprecatedCloud -> {
            encryptedPreferencesManager.clearProfileSnapshotFromCloudBackup()
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
            is BackupType.DeprecatedCloud, is BackupType.Cloud -> if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
                profile.toJson()
            } else {
                null
            }
            is BackupType.File.PlainText -> profile.toJson()
            is BackupType.File.Encrypted -> profile.toEncryptedJson(encryptionPassword = backupType.password)
        }
    }
}
