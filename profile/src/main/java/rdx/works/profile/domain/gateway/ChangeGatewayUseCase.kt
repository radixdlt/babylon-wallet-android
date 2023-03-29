package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class ChangeGatewayUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(gateway: Radix.Gateway) = profileDataSource
        .profile
        .firstOrNull()
        ?.let { profile ->
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
                profileDataSource.saveProfile(updatedProfile)
                true
            } else {
                false
            }
        } ?: false

}
