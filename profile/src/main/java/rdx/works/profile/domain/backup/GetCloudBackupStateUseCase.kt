package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.GoogleSignInManager
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
        cloudBackupErrorStream.errors
    ) { profile, lastCloudBackupEvent, backupError ->
        val email = googleSignInManager.getSignedInGoogleAccount()?.email
        if (profile.canBackupToCloud && email != null && backupError == null) {
            CloudBackupState.Enabled(email = email)
        } else {
            CloudBackupState.Disabled(
                email = email,
                lastCloudBackupTime = lastCloudBackupEvent?.cloudBackupTime
            )
        }
    }
}
