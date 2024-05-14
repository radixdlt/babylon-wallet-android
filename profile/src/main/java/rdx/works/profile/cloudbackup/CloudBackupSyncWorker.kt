package rdx.works.profile.cloudbackup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// NOTE: All background work is given a maximum of ten minutes to finish its execution.
// After this time has expired, the worker will be signalled to stop.
//
// Exercise caution in renaming classes derived from ListenableWorkers.
// WorkManager stores the class name in its internal database when the WorkRequest is enqueued
// so it can later create an instance of that worker when constraints are met. See link for more details:
// https://developer.android.com/reference/androidx/work/WorkManager#renaming-and-removing-listenableworker-classes
@HiltWorker
internal class CloudBackupSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted private val params: WorkerParameters,
    private val driveClient: DriveClient,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profile = profileRepository.inMemoryProfileOrNull
        val googleDriveFileId = preferencesManager.googleDriveFileId.first()

        return if (profile != null) {
            driveClient.backupProfile(
                googleDriveFileId = googleDriveFileId,
                profile = profile
            ).fold(
                onSuccess = { file ->
                    if (googleDriveFileId == null) { // TODO what if another sync comes and this is not done yet?
                        preferencesManager.setGoogleDriveFileId(file.id)
                        Timber.tag("CloudBackup").d("\uD83C\uDD95 ✅")
                    } else {
                        Timber.tag("CloudBackup").d("\uD83D\uDD04 ✅")
                    }
                    val modifiedTimeInstant = file.lastUsedOnDeviceModified.toInstant()
                    preferencesManager.updateLastCloudBackupInstant(modifiedTimeInstant)
                    Result.success()
                },
                onFailure = { exception ->
                    Timber.tag("CloudBackup").w(exception, "❌")
                    Result.failure()
                }
            )
        } else {
            Timber.tag("CloudBackup").d("❌ No profile snapshot")
            Result.failure()
        }
    }
}

@Singleton
class CloudBackupSyncExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    suspend fun syncProfile() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Every work request has a backoff policy and backoff delay.
        // The default policy is EXPONENTIAL with a delay of 30 seconds
        val workRequest = OneTimeWorkRequestBuilder<CloudBackupSyncWorker>()
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(context)

        if (preferencesManager.googleDriveFileId.firstOrNull() == null) {
            Timber.tag("CloudBackup").d("\uD83C\uDD95 Enqueued")
            workManager.enqueueUniqueWork(SYNC_CLOUD_PROFILE_WORK, ExistingWorkPolicy.KEEP, workRequest)
        } else {
            Timber.tag("CloudBackup").d("\uD83D\uDD04 Enqueued")
            // REPLACE existing work with the new work. This option cancels the existing work.
            workManager.enqueueUniqueWork(SYNC_CLOUD_PROFILE_WORK, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }

    companion object {
        private const val SYNC_CLOUD_PROFILE_WORK = "sync_cloud_profile"
    }
}