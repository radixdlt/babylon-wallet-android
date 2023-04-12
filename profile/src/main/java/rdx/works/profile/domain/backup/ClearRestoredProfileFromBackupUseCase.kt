package rdx.works.profile.domain.backup

import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class ClearRestoredProfileFromBackupUseCase @Inject constructor(
    val repository: ProfileRepository
) {

    suspend operator fun invoke() = repository.clearBackedUpProfile()
}
