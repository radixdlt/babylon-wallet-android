package rdx.works.profile.cloudbackup

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
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber

@Suppress("LongParameterList")
@HiltWorker
class CheckBackupStatusUseCase @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted private val params: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    private val driveClient: DriveClient,
    private val cloudBackupSyncExecutor: CloudBackupSyncExecutor,
    private val cloudBackupErrorStream: CloudBackupErrorStream
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Run this check only if the app has profile in memory, most frequently only when the app is in the foreground
        val profileState = profileRepository.profileState.filterNot {
            it is ProfileState.NotInitialised
        }.first()
        val profile = (profileState as? ProfileState.Restored)?.profile ?: return Result.success()
        if (!profile.canBackupToCloud) return Result.success()

        val lastBackupEvent = preferencesManager.lastCloudBackupEvent.firstOrNull()
        // TODO add check for migration modal?
        return if (lastBackupEvent == null || profile.header.lastModified > lastBackupEvent.profileModifiedTime) {
            cloudBackupSyncExecutor.requestCloudBackup()
            Result.success()
        } else {
            Timber.tag("CloudBackup").d("\uD83D\uDEDC Check Backup Status")
            driveClient.getCloudBackupEntity(fileId = lastBackupEvent.fileId, profile = profile).fold(
                onSuccess = {
                    Timber.tag("CloudBackup").d("\uD83D\uDEDC Check Backup Status: All good ✅")
                    // File still exists, no further action required
                    cloudBackupErrorStream.resetErrors()
                    Result.success()
                },
                onFailure = { exception ->
                    Timber.tag("CloudBackup").d(exception, "\uD83D\uDEDC Check Backup Status: Error ❌")
                    if (exception is BackupServiceException) {
                        cloudBackupErrorStream.onError(exception)
                    }

                    Result.success()
                }
            )
        }
    }
}
