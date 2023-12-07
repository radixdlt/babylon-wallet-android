package rdx.works.profile.domain

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddRecoveredAccountsToProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(accounts: List<Network.Account>): Profile {
        return profileRepository.updateProfile { profile ->
            val currentNetworkId = profile.currentNetwork.knownNetworkId ?: error("Current network is not known")
            profile.addAccounts(accounts, currentNetworkId)
        }
    }
}
