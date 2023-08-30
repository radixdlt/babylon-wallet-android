package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.updateThirdPartyDepositSettings
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class UpdateProfileThirdPartySettingsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        account: Network.Account,
        thirdPartyDeposits: Network.Account.OnLedgerSettings.ThirdPartyDeposits,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.updateThirdPartyDepositSettings(account, thirdPartyDeposits)
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
