package rdx.works.profile.domain.backup

import android.app.backup.BackupDataInputStream
import android.net.Uri
import rdx.works.core.storage.FileRepository
import rdx.works.core.then
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class SaveTemporaryRestoringSnapshotUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend fun forCloud(serializedProfile: String, backupType: BackupType.Cloud): Result<Unit> = backupProfileRepository
        .saveTemporaryRestoringSnapshot(
            snapshotSerialised = serializedProfile,
            backupType = backupType
        )

    @Deprecated("It is only used to fetch profile from old backup system.")
    suspend fun forDeprecatedCloud(data: BackupDataInputStream): Result<Unit> = runCatching {
        val byteArray = ByteArray(data.size())
        data.read(byteArray)
        byteArray.toString(Charsets.UTF_8)
    }.then { snapshot ->
        backupProfileRepository.saveTemporaryRestoringSnapshot(snapshot, BackupType.DeprecatedCloud) // TODO correct?
    }

    suspend fun forFile(uri: Uri, fileBackupType: BackupType.File): Result<Unit> {
        return fileRepository.read(fromFile = uri).then { content ->
            backupProfileRepository.saveTemporaryRestoringSnapshot(content, fileBackupType)
        }
    }
}
