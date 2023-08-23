package rdx.works.profile.domain.backup

import android.net.Uri
import rdx.works.core.storage.FileRepository
import rdx.works.profile.data.repository.ProfileRepository
import java.lang.Exception
import javax.inject.Inject

class ExportProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(exportType: ExportType, file: Uri): Result<Unit> {
        val snapshot = profileRepository.getSnapshotForFileBackup() ?: return Result.failure(Exception("Snapshot does not exist"))

        return when (exportType) {
            is ExportType.EncodedJson -> Result.success(Unit)
            is ExportType.Json -> fileRepository.save(toFile = file, data = snapshot)
        }
    }

    sealed interface ExportType {
        object Json: ExportType
        data class EncodedJson(val password: String): ExportType
    }
}
