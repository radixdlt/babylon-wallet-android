package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class CheckBackupStateWithCloudUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    private val cloudBackupSyncExecutor: CloudBackupSyncExecutor,
) {

    suspend operator fun invoke() {
        val currentProfile = profileRepository.profile.firstOrNull() ?: return
        if (!currentProfile.canBackupToCloud) return

        val lastBackupEvent = preferencesManager.lastCloudBackupEvent.firstOrNull()

        if (lastBackupEvent == null || currentProfile.header.lastModified > lastBackupEvent.profileModifiedTime) {
            cloudBackupSyncExecutor.requestCloudBackup()
        }
    }

}