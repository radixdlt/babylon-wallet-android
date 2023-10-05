package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class ChangeGatewayIfNetworkExistUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(gateway: Radix.Gateway) = profileRepository
        .profile
        .first()
        .let { profile ->
            val knownNetwork = Radix.Network
                .allKnownNetworks()
                .firstOrNull { network ->
                    network.name == gateway.network.name
                } ?: return@let false

            val networkExists = profile.networks.any { network ->
                network.networkID == knownNetwork.id
            }

            return@let if (networkExists) {
                val updatedProfile = profile.changeGateway(gateway)
                profileRepository.saveProfile(updatedProfile)
                true
            } else {
                false
            }
        }
}
