package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class UpdateDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(isEnabled: Boolean) {
        profileDataSource.updateDeveloperMode(isEnabled = isEnabled)
    }
}
