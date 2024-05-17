package rdx.works.profile.cloudbackup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rdx.works.core.domain.cloudbackup.LastBackupEvent
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.ApplicationScope
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
        val lastBackupEvent = preferencesManager.lastCloudBackupEvent.first()

        return if (profile != null) {
            driveClient.backupProfile(
                googleDriveFileId = lastBackupEvent?.fileId,
                profile = profile
            ).fold(
                onSuccess = { file ->
                    Timber.tag("CloudBackup").d("\uD83C\uDD95 ✅")
                    preferencesManager.updateLastBackupEvent(LastBackupEvent(
                        fileId = file.id,
                        profileModifiedTime = profile.header.lastModified,
                        cloudBackupTime = file.lastUsedOnDeviceModified
                    ))
                    Result.success()
                },
                onFailure = { exception ->
                    Timber.tag("CloudBackup").w(exception, "❌")
                    return when (exception) {
                        is BackupServiceException.ProfileClaimedByAnotherDeviceException -> {
                            profileRepository.clearAllWalletData()
                            Result.failure()
                        }
                        is BackupServiceException.UnauthorizedException -> {
                            Result.failure()
                        }
                        else -> {
                            if (runAttemptCount < 3) {
                                Timber.tag("CloudBackup").w(exception, "Retry: $runAttemptCount")
                                Result.retry()
                            } else {
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

@OptIn(FlowPreview::class)
@Singleton
class CloudBackupSyncExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val preferencesManager: PreferencesManager
) {
    private val syncProfile = MutableSharedFlow<Unit>()

    init {
        syncProfile
            .debounce(ONE_SECOND_DEBOUNCE)
            .onEach {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<CloudBackupSyncWorker>()
                    .setBackoffCriteria( // try every 10 seconds
                        backoffPolicy = BackoffPolicy.LINEAR,
                        backoffDelay = MIN_BACKOFF_MILLIS,
                        timeUnit = TimeUnit.MILLISECONDS
                    )
                    .setConstraints(constraints)
                    .build()

                val workManager = WorkManager.getInstance(context)

                if (preferencesManager.lastCloudBackupEvent.firstOrNull() == null) {
                    Timber.tag("CloudBackup").d("\uD83C\uDD95 Enqueued")
                    workManager.enqueueUniqueWork(SYNC_CLOUD_PROFILE_WORK, ExistingWorkPolicy.KEEP, workRequest)
                } else {
                    Timber.tag("CloudBackup").d("\uD83D\uDD04 Enqueued")
                    // REPLACE existing work with the new work. This option cancels the existing work.
                    workManager.enqueueUniqueWork(SYNC_CLOUD_PROFILE_WORK, ExistingWorkPolicy.REPLACE, workRequest)
                }
            }
            .launchIn(applicationScope)
    }
    suspend fun requestCloudBackup() {
        syncProfile.emit(Unit)
    }

    companion object {
        private const val SYNC_CLOUD_PROFILE_WORK = "sync_cloud_profile"
        private const val ONE_SECOND_DEBOUNCE = 1000L
    }
}
