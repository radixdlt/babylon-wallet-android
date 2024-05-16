package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.DriveClient
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCloudBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager,
    private val driveClient: DriveClient
) {

    operator fun invoke() = combine(
        profileRepository.profile,
        preferencesManager.lastCloudBackupInstant,
        driveClient.backupErrors
    ) { profile, lastCloudBackupInstant, backupError ->
        val email = googleSignInManager.getSignedInGoogleAccount()?.email
        if (profile.canBackupToCloud && email != null && backupError == null) {
            CloudBackupState.Enabled(email = email)
        } else {
            CloudBackupState.Disabled(
                email = email,
                lastCloudBackupTime = lastCloudBackupInstant
            )
        }
    }
}
