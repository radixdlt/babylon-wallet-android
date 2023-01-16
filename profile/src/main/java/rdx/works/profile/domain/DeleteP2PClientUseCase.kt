package rdx.works.profile.domain

import rdx.works.profile.data.extensions.deleteP2PClient
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class DeleteP2PClientUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(connectionPassword: String) {
        val profile = profileDataSource.readProfile()
        checkNotNull(profile) {
            "Profile does not exist"
        }

        val updatedProfile = profile.deleteP2PClient(
            connectionPassword = connectionPassword
        )
        // Save updated profile
        profileDataSource.saveProfile(updatedProfile)
    }
}
