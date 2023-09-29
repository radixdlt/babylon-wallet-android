package rdx.works.profile.domain.backup

import android.net.Uri
import rdx.works.core.storage.FileRepository
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class BackupProfileToFileUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(fileBackupType: BackupType.File, file: Uri): Result<Unit> {
        val snapshot = backupProfileRepository.getSnapshotForBackup(fileBackupType) ?: return Result.failure(
            Exception("Snapshot does not exist")
        )
        return fileRepository.save(toFile = file, data = snapshot)
    }

    fun deleteFile(file: Uri) {
        fileRepository.delete(file)
    }
}
