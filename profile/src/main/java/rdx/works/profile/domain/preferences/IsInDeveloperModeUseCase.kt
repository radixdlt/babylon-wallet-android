package rdx.works.profile.domain.preferences

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class IsInDeveloperModeUseCase @Inject constructor(
    val profileRepository: ProfileRepository
) {

    suspend operator fun invoke() = profileRepository
        .profile
        .first()
        .appPreferences
        .security
        .isDeveloperModeEnabled
}
