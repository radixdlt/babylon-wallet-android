package rdx.works.profile.cloudbackup.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber

@HiltWorker
class CheckBackupStatusUseCase @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted private val params: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    private val cloudBackupSyncExecutor: CloudBackupSyncExecutor,
    private val checkCloudBackupFileAvailabilityUseCase: CheckCloudBackupFileAvailabilityUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Run this check only if the app has profile in memory, most frequently only when the app is in the foreground
        val profileState = profileRepository.profileState.filterNot {
            it is ProfileState.NotInitialised
        }.first()
        val profile = (profileState as? ProfileState.Restored)?.profile ?: return Result.success()

        val lastCloudBackupEvent = preferencesManager.lastCloudBackupEvent.firstOrNull()

        return if (
            profile.canBackupToCloud &&
            (lastCloudBackupEvent == null || profile.header.lastModified > lastCloudBackupEvent.profileModifiedTime)
        ) {
            cloudBackupSyncExecutor.requestCloudBackup()
            Result.success()
        } else {
            Timber.tag("CloudBackup").d("\uD83D\uDEDC Check Backup Status")
            return lastCloudBackupEvent?.let {
                checkCloudBackupFileAvailabilityUseCase(profile)
            } ?: Result.success()
        }
    }
}
