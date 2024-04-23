package rdx.works.profile.domain.gateway

import com.radixdlt.sargon.Gateway
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.deleteGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class DeleteGatewayUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(gateway: Gateway) = profileRepository
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.deleteGateway(gateway)
            profileRepository.saveProfile(updatedProfile)
        }
}
