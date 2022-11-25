package rdx.works.profile.domain

import rdx.works.profile.data.extensions.addP2PClient
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class AddP2PClientUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(
        displayName: String,
        connectionPassword: String
    ) {

        // Read profile first as its needed to create account
        profileRepository.readProfile()?.let { profile ->

            // Add p2p client to the profile
            val updatedProfile = profile.addP2PClient(
                connectionPassword = connectionPassword,
                displayName = displayName
            )

            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
        }
    }
}