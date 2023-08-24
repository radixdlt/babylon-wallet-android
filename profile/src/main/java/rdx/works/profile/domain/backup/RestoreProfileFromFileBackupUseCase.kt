package rdx.works.profile.domain.backup

import android.net.Uri
import rdx.works.core.InstantGenerator
import rdx.works.core.storage.FileRepository
import rdx.works.core.then
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class RestoreProfileFromFileBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(uri: Uri, password: String?): Result<Unit> {
        return fileRepository.read(fromFile = uri).then { content ->
            backupProfileRepository.saveRestoringSnapshotFromFileBackup(content, password)
        }
    }
}
