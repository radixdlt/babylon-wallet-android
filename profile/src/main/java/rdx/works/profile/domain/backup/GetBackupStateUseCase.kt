package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager,
    private val cloudBackupErrorStream: CloudBackupErrorStream
) {

    operator fun invoke(): Flow<BackupState> = combine(
        profileRepository.profile,
        preferencesManager.lastCloudBackupEvent,
        cloudBackupErrorStream.errors,
        preferencesManager.lastManualBackupInstant
    ) { profile, lastCloudBackupEvent, backupError, lastManualBackupInstant ->
        val email = googleSignInManager.getSignedInGoogleAccount()?.email

        if (profile.canBackupToCloud && email != null && backupError == null) {
            BackupState.CloudBackupEnabled(email = email)
        } else {
            val isServiceError = (backupError is BackupServiceException.ServiceException || backupError is BackupServiceException.Unknown)

            if (isServiceError && profile.canBackupToCloud && email != null) {
                BackupState.CloudBackupEnabled(
                    email = email,
                    hasAnyErrors = true,
                    lastCloudBackupTime = lastCloudBackupEvent?.cloudBackupTime,
                    lastManualBackupTime = lastManualBackupInstant
                )
            } else {
                BackupState.CloudBackupDisabled(
                    email = email,
                    lastCloudBackupTime = lastCloudBackupEvent?.cloudBackupTime,
                    lastManualBackupTime = lastManualBackupInstant,
                    lastModifiedProfileTime = profile.header.lastModified
                )
            }
        }
    }
}
