package rdx.works.profile.domain.depositguarantees

import com.radixdlt.sargon.Decimal192
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.changeDefaultDepositGuarantee
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeDefaultDepositGuaranteeUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        defaultDepositGuarantee: Decimal192
    ) {
        withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.changeDefaultDepositGuarantee(
                defaultDepositGuarantee = defaultDepositGuarantee
            )
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
