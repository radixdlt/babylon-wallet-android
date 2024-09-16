package rdx.works.profile.cloudbackup.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.radixdlt.sargon.ProfileState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
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
    private val cloudBackupErrorStream: CloudBackupErrorStream
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profileState = profileRepository.profileState.first()
        val profile = (profileState as? ProfileState.Loaded)?.v1

        return if (profile != null) {
            driveClient.backupProfile(profile = profile).fold(
                onSuccess = {
                    Timber.tag("CloudBackup").d("\uD83C\uDD95 ✅")
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
