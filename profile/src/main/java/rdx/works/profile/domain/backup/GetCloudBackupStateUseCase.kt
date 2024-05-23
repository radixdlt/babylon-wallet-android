package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCloudBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager,
    private val cloudBackupErrorStream: CloudBackupErrorStream
) {

    operator fun invoke(): Flow<CloudBackupState> = combine(
        profileRepository.profile,
        preferencesManager.lastCloudBackupEvent,
        cloudBackupErrorStream.errors,
        preferencesManager.lastManualBackupInstant
    ) { profile, lastCloudBackupEvent, backupError, lastManualBackupInstant ->
        val email = googleSignInManager.getSignedInGoogleAccount()?.email

        if (profile.canBackupToCloud && email != null && backupError == null) {
            CloudBackupState.Enabled(email = email)
        } else {
            if (email != null &&
                (backupError is BackupServiceException.ServiceException || backupError is BackupServiceException.Unknown)
            ) {
                CloudBackupState.Enabled(
                    email = email,
                    hasAnyErrors = true,
                    lastCloudBackupTime = lastCloudBackupEvent?.cloudBackupTime,
                    lastManualBackupTime = lastManualBackupInstant
                )
            } else {
                CloudBackupState.Disabled(
                    email = email,
                    lastCloudBackupTime = lastCloudBackupEvent?.cloudBackupTime,
                    lastManualBackupTime = lastManualBackupInstant
                )
            }
        }
    }
}
