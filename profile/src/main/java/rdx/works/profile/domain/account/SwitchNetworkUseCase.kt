package rdx.works.profile.domain.account

import com.radixdlt.sargon.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.addNetworkIfDoesNotExist
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class SwitchNetworkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(networkUrl: Url) = withContext(defaultDispatcher) {
        val profile = profileRepository.profile.first()

        val gateway = profile.appPreferences.gateways.other.asIdentifiable().getBy(networkUrl) ?: return@withContext
        val updatedProfile = profile.addNetworkIfDoesNotExist(gateway.network.id).changeGateway(gateway)
        profileRepository.saveProfile(updatedProfile)
    }
}
