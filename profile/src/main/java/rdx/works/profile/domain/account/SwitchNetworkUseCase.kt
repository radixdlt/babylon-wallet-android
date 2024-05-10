package rdx.works.profile.domain.account

import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.getBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.addNetworkIfDoesNotExist
import rdx.works.core.sargon.changeGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class SwitchNetworkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(networkUrl: Url) = withContext(defaultDispatcher) {
        val profile = profileRepository.profile.first()

        val gateway = profile.appPreferences.gateways.other.getBy(networkUrl) ?: return@withContext
        val updatedProfile = profile.addNetworkIfDoesNotExist(gateway.network.id).changeGateway(gateway)
        profileRepository.saveProfile(updatedProfile)
    }
}
