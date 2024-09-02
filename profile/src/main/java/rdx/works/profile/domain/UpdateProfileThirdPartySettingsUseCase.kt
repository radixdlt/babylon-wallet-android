package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ThirdPartyDeposits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.updateThirdPartyDepositSettings
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class UpdateProfileThirdPartySettingsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        account: Account,
        thirdPartyDeposits: ThirdPartyDeposits,
    ) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.updateThirdPartyDepositSettings(account, thirdPartyDeposits)
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
