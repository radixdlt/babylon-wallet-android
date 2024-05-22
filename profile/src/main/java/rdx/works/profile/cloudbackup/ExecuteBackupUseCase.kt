package rdx.works.profile.cloudbackup

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
                            cloudBackupTime = file.lastUsedOnDeviceModified
                        )
                    )
                    cloudBackupErrorStream.resetErrors()
                    Result.success()
                },
                onFailure = { exception ->
                    return when (exception) {
                        is BackupServiceException.ClaimedByAnotherDevice -> {
                            profileRepository.clearAllWalletData()
                            cloudBackupErrorStream.onError(exception)
                            Timber.tag("CloudBackup").w(exception, "❌")
                            Result.failure()
                        }
                        is BackupServiceException.UnauthorizedException -> {
                            cloudBackupErrorStream.onError(exception)
                            Timber.tag("CloudBackup").w(exception, "❌")
                            Result.failure()
                        }
                        else -> {
                            if (runAttemptCount < 3) {
                                Timber.tag("CloudBackup").w(exception, "❌ Retry: $runAttemptCount")
                                Result.retry()
                            } else {
                                Timber.tag("CloudBackup").w(exception, "❌")
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
