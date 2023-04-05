package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class IsInDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke() = profileDataSource.isInDeveloperMode()
}
