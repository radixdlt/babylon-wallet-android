package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileDataSource: ProfileDataSource) {

    operator fun invoke() = profileDataSource.profile
}
