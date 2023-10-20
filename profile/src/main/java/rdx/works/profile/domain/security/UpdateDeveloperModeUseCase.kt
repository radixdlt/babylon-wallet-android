package rdx.works.profile.domain.security

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.extensions.updateDeveloperMode
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class UpdateDeveloperModeUseCase @Inject constructor(
    val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(isEnabled: Boolean) = profileRepository
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.updateDeveloperMode(isEnabled)
            profileRepository.saveProfile(updatedProfile)
        }
}
