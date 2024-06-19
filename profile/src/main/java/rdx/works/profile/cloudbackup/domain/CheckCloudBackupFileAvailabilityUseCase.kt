package rdx.works.profile.cloudbackup.domain

import androidx.work.ListenableWorker.Result
import com.radixdlt.sargon.Profile
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.DriveClient
import rdx.works.profile.cloudbackup.model.BackupServiceException
import timber.log.Timber
import javax.inject.Inject

class CheckCloudBackupFileAvailabilityUseCase @Inject constructor(
    private val driveClient: DriveClient,
    private val preferencesManager: PreferencesManager,
    private val cloudBackupErrorStream: CloudBackupErrorStream
) {
    suspend operator fun invoke(profile: Profile): Result {
        val lastCloudBackupEvent = preferencesManager.lastCloudBackupEvent.firstOrNull()
        return lastCloudBackupEvent?.let {
            driveClient.getCloudBackupEntity(
                fileId = lastCloudBackupEvent.fileId,
                profile = profile
            ).fold(
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
        } ?: Result.success()
    }
}
