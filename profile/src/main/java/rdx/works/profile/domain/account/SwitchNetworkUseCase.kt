package rdx.works.profile.domain.account

import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.sargon.addNetworkIfDoesNotExist
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class SwitchNetworkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(networkId: NetworkId) = withContext(defaultDispatcher) {
        val profile = profileRepository.profile.first()

        val updatedProfile = profile.changeGatewayToNetworkId(networkId = networkId)
        profileRepository.saveProfile(updatedProfile)
    }
}
