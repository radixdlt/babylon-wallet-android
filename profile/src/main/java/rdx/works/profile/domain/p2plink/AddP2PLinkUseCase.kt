package rdx.works.profile.domain.p2plink

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.addP2PLink
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class AddP2PLinkUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource,
) {

    suspend operator fun invoke(
        displayName: String,
        connectionPassword: String
    ) {
        val profile = profileDataSource.profile.firstOrNull()
        checkNotNull(profile) {
            "Profile does not exist"
        }

        val p2pLink = P2PLink.init(
            connectionPassword = connectionPassword,
            displayName = displayName
        )

        // Add p2p client to the profile
        val updatedProfile = profile.addP2PLink(
            p2pLink = p2pLink
        )

        // Save updated profile
        profileDataSource.saveProfile(updatedProfile)
    }
}
