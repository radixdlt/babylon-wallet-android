package rdx.works.profile.domain.p2plink

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.extensions.deleteP2PLink
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.datastore.EncryptedPreferencesManager
import javax.inject.Inject

class DeleteP2PLinkUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val encryptedPreferencesManager: EncryptedPreferencesManager
) {

    suspend operator fun invoke(publicKey: String) {
        val profile = profileRepository.profile.first()

        val updatedProfile = profile.deleteP2PLink(
            publicKey = publicKey
        )
        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
        encryptedPreferencesManager.removeP2PLinkKeys(publicKey)
    }
}
