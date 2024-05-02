package rdx.works.profile.domain.p2plink

import com.radixdlt.sargon.P2pLink
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.deleteP2PLink
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class DeleteP2PLinkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(p2pLink: P2pLink) {
        val profile = profileRepository.profile.first()

        val updatedProfile = profile.deleteP2PLink(p2pLink = p2pLink)
        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
    }
}
