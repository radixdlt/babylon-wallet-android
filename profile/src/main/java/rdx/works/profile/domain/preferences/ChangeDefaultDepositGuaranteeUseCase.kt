package rdx.works.profile.domain.preferences

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.changeDefaultDepositGuarantee
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeDefaultDepositGuaranteeUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        defaultDepositGuarantee: Double
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
