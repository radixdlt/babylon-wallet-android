package rdx.works.profile.domain

import rdx.works.profile.data.extensions.addP2PClient
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class AddP2PClientUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(
        displayName: String,
        connectionPassword: String
    ) {
        val profile = profileRepository.readProfile()
        checkNotNull(profile) {
            "Profile does not exist"
        }

        val p2pClient = P2PClient.init(
            connectionPassword = connectionPassword,
            displayName = displayName
        )

        // Add p2p client to the profile
        val updatedProfile = profile.addP2PClient(
            p2pClient = p2pClient
        )

        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
    }
}
