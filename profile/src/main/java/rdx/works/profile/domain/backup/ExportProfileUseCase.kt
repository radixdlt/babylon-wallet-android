package rdx.works.profile.domain.backup

import android.net.Uri
import rdx.works.core.storage.FileRepository
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class ExportProfileUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(exportType: ExportType, file: Uri): Result<Unit> {
        val snapshot = backupProfileRepository.getSnapshotForFileBackup(exportType) ?: return Result.failure(
            Exception("Snapshot does not exist")
        )
        return fileRepository.save(toFile = file, data = snapshot)
    }
}

sealed interface ExportType {
    object Json: ExportType
    data class EncodedJson(val password: String): ExportType
}
