package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCurrentGatewayUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke() = profileDataSource
        .profile
        .first().appPreferences.gateways.current()
}
