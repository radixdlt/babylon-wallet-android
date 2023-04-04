package rdx.works.profile.domain.p2plink

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.deleteP2PLink
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class DeleteP2PLinkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(connectionPassword: String) {
        val profile = profileRepository.profile.first()

        val updatedProfile = profile.deleteP2PLink(
            connectionPassword = connectionPassword
        )
        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
    }
}
