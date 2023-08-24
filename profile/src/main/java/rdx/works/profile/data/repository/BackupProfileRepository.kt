package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.EncryptedProfileSnapshot
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.ProfileSerializer
import rdx.works.profile.domain.InvalidPasswordException
import rdx.works.profile.domain.InvalidSnapshotException
import rdx.works.profile.domain.backup.ExportType
import java.lang.Exception
import javax.inject.Inject

interface BackupProfileRepository {

    suspend fun saveRestoringSnapshotFromCloudBackup(snapshotSerialised: String): Result<Unit>

    suspend fun saveRestoringSnapshotFromFileBackup(content: String, password: String?): Result<Unit>

    suspend fun getSnapshotForCloudBackup(): String?

    suspend fun getSnapshotForFileBackup(exportType: ExportType): String?

    suspend fun getRestoringProfileFromBackup(): Profile?

    suspend fun discardBackedUpProfile()
}

class BackupProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val preferencesManager: PreferencesManager,
    @ProfileSerializer private val profileSnapshotJson: Json,
    private val profileRepository: ProfileRepository
) : BackupProfileRepository {

    override suspend fun saveRestoringSnapshotFromCloudBackup(snapshotSerialised: String): Result<Unit> =
        if (profileRepository.deriveProfileState(snapshotSerialised) is ProfileState.Restored) {
            encryptedPreferencesManager.putProfileSnapshotFromBackup(snapshotSerialised)
            preferencesManager.updateLastBackupInstant(InstantGenerator())
            Result.success(Unit)
        } else {
            Result.failure(InvalidSnapshotException)
        }

    override suspend fun saveRestoringSnapshotFromFileBackup(content: String, password: String?): Result<Unit> {
        return if (password != null) {
            val encryptedProfileSnapshot = runCatching {
                profileSnapshotJson.decodeFromString<EncryptedProfileSnapshot>(content)
            }.getOrNull()

            if (encryptedProfileSnapshot == null) {
                Result.failure(InvalidSnapshotException)
            } else {
                val snapshot = runCatching {
                    val decrypted = encryptedProfileSnapshot.decrypt(profileSnapshotJson, password)
                    profileSnapshotJson.encodeToString(decrypted)
                }.getOrNull()

                if (snapshot != null) {
                    encryptedPreferencesManager.putProfileSnapshotFromBackup(snapshot)
                    Result.success(Unit)
                } else {
                    Result.failure(InvalidPasswordException)
                }
            }
        } else {
            val profileState = profileRepository.deriveProfileState(content)
            if (profileState is ProfileState.Restored) {
                encryptedPreferencesManager.putProfileSnapshotFromBackup(content)
                Result.success(Unit)
            } else {
                val encryptedProfileSnapshot = runCatching {
                    profileSnapshotJson.decodeFromString<EncryptedProfileSnapshot>(content)
                }.getOrNull()

                if (encryptedProfileSnapshot != null) {
                    Result.failure(InvalidPasswordException)
                } else {
                    Result.failure(InvalidSnapshotException)
                }
            }
        }
    }

    override suspend fun getSnapshotForCloudBackup(): String? {
        val profile = profileRepository.profile.firstOrNull() ?: return null

        return if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
            runCatching { profileSnapshotJson.encodeToString(profile.snapshot()) }.onSuccess {
                preferencesManager.updateLastBackupInstant(InstantGenerator())
            }.getOrNull()
        } else {
            null
        }
    }

    override suspend fun getSnapshotForFileBackup(exportType: ExportType): String? {
        val profileSnapshot = profileRepository.profile.firstOrNull()?.snapshot() ?: return null

        val snapshotString = runCatching { profileSnapshotJson.encodeToString(profileSnapshot) }.getOrNull() ?: return null

        return when (exportType) {
            is ExportType.EncodedJson -> {
                val encryptedSnapshot = EncryptedProfileSnapshot.from(snapshotString, exportType.password)
                runCatching { profileSnapshotJson.encodeToString(encryptedSnapshot) }.getOrNull()
            }

            is ExportType.Json -> snapshotString
        }
    }

    override suspend fun getRestoringProfileFromBackup(): Profile? {
        val state = encryptedPreferencesManager.getProfileSnapshotFromBackup()?.let { snapshot ->
            profileRepository.deriveProfileState(snapshot)
        }

        return (state as? ProfileState.Restored)?.profile
    }

    override suspend fun discardBackedUpProfile() {
        encryptedPreferencesManager.clearProfileSnapshotFromBackup()
        preferencesManager.removeLastBackupInstant()
    }

}
