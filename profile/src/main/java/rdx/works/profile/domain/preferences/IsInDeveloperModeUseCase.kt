package rdx.works.profile.domain.preferences

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class IsInDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke() = profileDataSource
        .profile
        .first()
        .appPreferences
        .security
        .isDeveloperModeEnabled
}
