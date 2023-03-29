package rdx.works.profile.domain.preferences

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.apppreferences.addGateway
import rdx.works.profile.data.model.apppreferences.updateDeveloperMode
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class UpdateDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(isEnabled: Boolean) = profileDataSource
        .profile
        .firstOrNull()
        ?.let { profile ->
            val updatedProfile = profile.updateDeveloperMode(isEnabled)
            profileDataSource.saveProfile(updatedProfile)
        }
}
