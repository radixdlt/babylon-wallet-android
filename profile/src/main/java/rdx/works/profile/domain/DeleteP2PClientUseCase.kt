package rdx.works.profile.domain

import rdx.works.profile.data.extensions.deleteP2PClient
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeleteP2PClientUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(
        connectionPassword: String
    ) {
        val profileSnapshot = profileRepository.readProfileSnapshot()
        checkNotNull(profileSnapshot) {
            "Profile does not exist"
        }

        val profile = profileSnapshot.toProfile()
        val updatedProfile = profile.deleteP2PClient(
            connectionPassword = connectionPassword
        )
        // Save updated profile
        profileRepository.saveProfileSnapshot(updatedProfile.snapshot())
    }
}
