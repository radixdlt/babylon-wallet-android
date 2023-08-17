package rdx.works.profile.domain

import rdx.works.core.KeystoreManager
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(private val dataSource: ProfileRepository, private val keystoreManager: KeystoreManager) {

    suspend operator fun invoke() {
        dataSource.clear()
        keystoreManager.removeKeys()
    }
}
