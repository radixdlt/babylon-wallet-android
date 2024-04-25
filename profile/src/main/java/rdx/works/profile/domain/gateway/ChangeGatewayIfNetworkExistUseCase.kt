package rdx.works.profile.domain.gateway

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.extensions.invoke
import kotlinx.coroutines.flow.first
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
            val networkExists = profile.networks().any { network -> network.id == gateway.network.id }

            return@let if (networkExists) {
                val updatedProfile = profile.changeGateway(gateway)
                profileRepository.saveProfile(updatedProfile)
                true
            } else {
                false
            }
        }
}
