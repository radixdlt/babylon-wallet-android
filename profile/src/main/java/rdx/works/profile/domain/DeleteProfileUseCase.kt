package rdx.works.profile.domain

import com.radixdlt.sargon.extensions.serializedJsonString
import rdx.works.core.KeystoreManager
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.backup.BackupType
import timber.log.Timber
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val backupProfileRepository: BackupProfileRepository,
    private val keystoreManager: KeystoreManager
) {

    suspend operator fun invoke() {
        val profileThatWasBackedUpToCloud = backupProfileRepository.getTemporaryRestoringProfile(BackupType.Cloud)

        profileRepository.clearAllWalletData()
        keystoreManager.removeKeys().onFailure {
            Timber.d(it, "Failed to delete encryption keys")
        }

        if (profileThatWasBackedUpToCloud != null) {
            // Rescuing the copy of the cloud backup so as the user
            // can see it and restore it, even after deleting the profile and the keys.
            backupProfileRepository.saveTemporaryRestoringSnapshot(
                snapshotSerialised = profileThatWasBackedUpToCloud.serializedJsonString(),
                backupType = BackupType.Cloud
            )
        }
    }

    suspend fun deleteProfileDataOnly() {
        profileRepository.clearProfileDataOnly()
    }
}
