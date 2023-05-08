package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class SwitchNetworkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        networkUrl: String,
        networkName: String
    ): NetworkId {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val gateway = Radix.Gateway(
                url = networkUrl,
                network = Radix.Network.allKnownNetworks().first { network ->
                    network.name == networkName
                }
            )
            val updatedProfile = profile.changeGateway(gateway)
            profileRepository.saveProfile(updatedProfile)
            gateway.network.networkId()
        }
    }
}
