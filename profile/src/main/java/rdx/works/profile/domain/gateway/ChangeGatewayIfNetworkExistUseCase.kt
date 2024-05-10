package rdx.works.profile.domain.gateway

import com.radixdlt.sargon.Gateway
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class ChangeGatewayIfNetworkExistUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(gateway: Gateway) = profileRepository
        .profile
        .first()
        .let { profile ->
            val networkExists = profile.networks.asIdentifiable().getBy(gateway.network.id) != null

            return@let if (networkExists) {
                val updatedProfile = profile.changeGateway(gateway)
                profileRepository.saveProfile(updatedProfile)
                true
            } else {
                false
            }
        }
}
