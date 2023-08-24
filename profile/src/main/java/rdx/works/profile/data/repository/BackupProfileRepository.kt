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
import rdx.works.profile.domain.backup.ExportType
import javax.inject.Inject

interface BackupProfileRepository {

    suspend fun saveRestoringSnapshotFromCloud(snapshotSerialised: String): Boolean

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
): BackupProfileRepository {

    override suspend fun saveRestoringSnapshotFromCloud(snapshotSerialised: String): Boolean =
        if (profileRepository.deriveProfileState(snapshotSerialised) is ProfileState.Restored) {
            encryptedPreferencesManager.putProfileSnapshotFromBackup(snapshotSerialised)
            preferencesManager.updateLastBackupInstant(InstantGenerator())
            true
        } else {
            false
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
