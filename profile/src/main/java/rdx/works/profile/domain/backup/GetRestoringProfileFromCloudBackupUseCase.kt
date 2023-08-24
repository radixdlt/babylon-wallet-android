package rdx.works.profile.domain.backup

import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class GetRestoringProfileFromCloudBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository
) {

    suspend operator fun invoke() = backupProfileRepository.getRestoringProfileFromCloudBackup()
}
