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
    ): P2PClient {

        val profileSnapshot = profileRepository.readProfileSnapshot()
        checkNotNull(profileSnapshot) {
            "Profile does not exist"
        }

        val profile = profileSnapshot.toProfile()

        val p2pClient = P2PClient.init(
            connectionPassword = connectionPassword,
            displayName = displayName
        )

        // Add p2p client to the profile
        val updatedProfile = profile.addP2PClient(
            p2pClient = p2pClient
        )

        // Save updated profile
        profileRepository.saveProfileSnapshot(updatedProfile.snapshot())

        return p2pClient
    }
}
