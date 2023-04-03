package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetGatewaysUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    operator fun invoke() = profileDataSource
        .profile
        .map { profile ->
            profile.appPreferences.gateways
        }
}
