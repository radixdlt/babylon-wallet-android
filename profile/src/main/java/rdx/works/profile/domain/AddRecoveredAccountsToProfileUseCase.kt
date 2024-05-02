package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Profile
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.currentNetwork
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddRecoveredAccountsToProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(accounts: List<Account>): Profile {
        return profileRepository.updateProfile { profile ->
            val currentNetworkId = profile.currentNetwork?.id ?: error("Current network is not known")
            profile.addAccounts(accounts, currentNetworkId)
        }
    }
}
