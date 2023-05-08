package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCurrentGatewayUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke() = profileRepository
        .profile
        .first()
        .currentGateway
}
