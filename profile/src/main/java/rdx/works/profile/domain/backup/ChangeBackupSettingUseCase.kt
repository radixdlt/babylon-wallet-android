package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.updateCloudSyncEnabled
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class ChangeBackupSettingUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(isChecked: Boolean) = profileRepository.profile
        .first()
        .let { profile ->
            val mutated = profile.updateCloudSyncEnabled(isChecked)
            profileRepository.saveProfile(mutated)
        }
}
