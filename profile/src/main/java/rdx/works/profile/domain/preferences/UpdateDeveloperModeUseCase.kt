package rdx.works.profile.domain.preferences

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.updateDeveloperMode
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class UpdateDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(isEnabled: Boolean) = profileDataSource
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.updateDeveloperMode(isEnabled)
            profileDataSource.saveProfile(updatedProfile)
        }
}
