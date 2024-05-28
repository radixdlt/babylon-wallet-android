package rdx.works.profile.cloudbackup.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.ProfileState
import rdx.works.core.domain.cloudbackup.LastCloudBackupEvent
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.DriveClient
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber

@HiltWorker
class ExecuteBackupUseCase @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted private val params: WorkerParameters,
    private val driveClient: DriveClient,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val cloudBackupErrorStream: CloudBackupErrorStream
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profileState = profileRepository.profileState.filterNot {
            it is ProfileState.NotInitialised
        }.first()
        val profile = (profileState as? ProfileState.Restored)?.profile
        val lastBackupEvent = preferencesManager.lastCloudBackupEvent.first()

        return if (profile != null) {
            driveClient.backupProfile(
                googleDriveFileId = lastBackupEvent?.fileId,
                profile = profile
            ).fold(
                onSuccess = { file ->
                    Timber.tag("CloudBackup").d("\uD83C\uDD95 ✅")
                    preferencesManager.updateLastCloudBackupEvent(
                        LastCloudBackupEvent(
                            fileId = file.id,
                            profileModifiedTime = profile.header.lastModified,
                            cloudBackupTime = file.lastBackup
                        )
                    )
                    cloudBackupErrorStream.resetErrors()
                    Result.success()
                },
                onFailure = { exception ->
                    Timber.tag("CloudBackup").w(exception, "❌")
                    return when (exception) {
                        is BackupServiceException.ClaimedByAnotherDevice, is BackupServiceException.UnauthorizedException -> {
                            cloudBackupErrorStream.onError(exception as BackupServiceException)
                            Result.failure()
                        }
                        else -> {
                            if (runAttemptCount < 3) {
                                Timber.tag("CloudBackup").d("Retry: $runAttemptCount")
                                Result.retry()
                            } else {
                                if (exception is BackupServiceException) {
                                    cloudBackupErrorStream.onError(exception)
                                }
                                Result.failure()
                            }
                        }
                    }
                }
            )
        } else {
            Timber.tag("CloudBackup").d("❌ No profile snapshot")
            Result.failure()
        }
    }
}
