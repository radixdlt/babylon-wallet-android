package rdx.works.profile.domain.gateway

import com.radixdlt.sargon.Gateway
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.addGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddGatewayUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(gateway: Gateway) = profileRepository
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.addGateway(gateway)
            profileRepository.saveProfile(updatedProfile)
        }
}
