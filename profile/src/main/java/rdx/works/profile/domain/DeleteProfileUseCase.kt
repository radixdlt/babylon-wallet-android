package rdx.works.profile.domain

import rdx.works.core.KeystoreManager
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(private val dataSource: ProfileRepository, private val keystoreManager: KeystoreManager) {

    suspend operator fun invoke() {
        dataSource.clear()
        keystoreManager.removeKeys().onFailure {
            Timber.d(it, "Failed to delete encryption keys")
        }
    }
}
