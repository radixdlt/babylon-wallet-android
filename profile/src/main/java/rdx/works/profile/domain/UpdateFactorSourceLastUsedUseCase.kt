package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.updateLastUsed
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class UpdateFactorSourceLastUsedUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(factorSourceId: FactorSourceId) {
        val profile = profileRepository.profile.first()
        profileRepository.saveProfile(profile.updateLastUsed(factorSourceId))
    }
}
