package rdx.works.profile.domain.backup

import android.app.backup.BackupDataInputStream
import android.net.Uri
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.checkIfEncryptedProfileJsonContainsLegacyP2PLinks
import com.radixdlt.sargon.extensions.checkIfProfileJsonContainsLegacyP2PLinks
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.storage.FileRepository
import rdx.works.core.then
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class SaveTemporaryRestoringSnapshotUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend fun forCloud(serializedProfile: String, backupType: BackupType.Cloud): Result<Unit> {
        ensureP2PLinkMigrationAcknowledged(serializedProfile, backupType)
        return backupProfileRepository.saveTemporaryRestoringSnapshot(
            snapshotSerialised = serializedProfile,
            backupType = backupType
        )
    }

    @Deprecated("It is only used to fetch profile from old backup system.")
    suspend fun forDeprecatedCloud(data: BackupDataInputStream): Result<Unit> = runCatching {
        val byteArray = ByteArray(data.size())
        data.read(byteArray)
        byteArray.toString(Charsets.UTF_8)
    }.then { snapshot ->
        backupProfileRepository.saveTemporaryRestoringSnapshot(snapshot, BackupType.DeprecatedCloud)
    }

    suspend fun forFile(uri: Uri, fileBackupType: BackupType.File): Result<Unit> {
        return fileRepository.read(fromFile = uri).then { content ->
            ensureP2PLinkMigrationAcknowledged(content, fileBackupType)
            backupProfileRepository.saveTemporaryRestoringSnapshot(content, fileBackupType)
        }
    }

    private suspend fun ensureP2PLinkMigrationAcknowledged(content: String, backupType: BackupType) {
        val containsLegacyP2PLinks = when (backupType) {
            is BackupType.File.Encrypted -> Profile.checkIfEncryptedProfileJsonContainsLegacyP2PLinks(
                jsonString = content,
                password = backupType.password
            )

            else -> Profile.checkIfProfileJsonContainsLegacyP2PLinks(
                jsonString = content
            )
        }
        if (containsLegacyP2PLinks) {
            preferencesManager.setShowRelinkConnectorsAfterProfileRestore(true)
        }
    }
}
