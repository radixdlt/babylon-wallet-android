package rdx.works.profile.domain.backup

import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    val repository: ProfileRepository
) {

    suspend operator fun invoke() {
        val profile = repository.getRestoredProfileFromBackup()

        if (profile != null) {
            repository.saveProfile(profile)
            repository.clearBackedUpProfile()
        }
    }
}
