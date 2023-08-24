package rdx.works.profile.domain.backup

import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class GetRestoringProfileFromBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository
) {

    suspend operator fun invoke() = backupProfileRepository.getRestoringProfileFromBackup()
}
