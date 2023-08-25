package rdx.works.profile.domain.backup

import android.net.Uri
import rdx.works.core.storage.FileRepository
import rdx.works.core.then
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class SaveTemporaryRestoringSnapshotUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend fun forCloud(snapshot: String): Result<Unit> {
        return backupProfileRepository.saveTemporaryRestoringSnapshot(snapshot, BackupType.Cloud)
    }

    suspend fun forFile(uri: Uri, fileBackupType: BackupType.File): Result<Unit> {
        return fileRepository.read(fromFile = uri).then { content ->
            backupProfileRepository.saveTemporaryRestoringSnapshot(content, fileBackupType)
        }
    }
}
