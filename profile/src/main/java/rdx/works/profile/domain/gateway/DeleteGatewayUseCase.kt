package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.deleteGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class DeleteGatewayUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(gateway: Radix.Gateway) = profileRepository
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.deleteGateway(gateway)
            profileRepository.saveProfile(updatedProfile)
        }
}
