package rdx.works.profile.domain.p2plink

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.addP2PLink
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddP2PLinkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(
        displayName: String,
        connectionPassword: String
    ) {
        val profile = profileRepository.profile.first()

        val p2pLink = P2PLink.init(
            connectionPassword = connectionPassword,
            displayName = displayName
        )

        // Add p2p client to the profile
        val updatedProfile = profile.addP2PLink(
            p2pLink = p2pLink
        )

        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
    }
}
