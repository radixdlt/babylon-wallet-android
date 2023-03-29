package rdx.works.profile.domain.p2plink

import rdx.works.profile.data.model.apppreferences.deleteP2PLink
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class DeleteP2PLinkUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(connectionPassword: String) {
        val profile = profileDataSource.readProfile()
        checkNotNull(profile) {
            "Profile does not exist"
        }

        val updatedProfile = profile.deleteP2PLink(
            connectionPassword = connectionPassword
        )
        // Save updated profile
        profileDataSource.saveProfile(updatedProfile)
    }
}
