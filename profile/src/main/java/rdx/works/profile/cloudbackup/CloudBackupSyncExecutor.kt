package rdx.works.profile.cloudbackup

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.di.coroutines.ApplicationScope
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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

                val workRequest = OneTimeWorkRequestBuilder<ExecuteBackupUseCase>()
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

    fun startPeriodicChecks(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(PeriodicChecksLifecycleObserver(WorkManager.getInstance(context)))
    }

    private class PeriodicChecksLifecycleObserver(
        private val workManager: WorkManager
    ): DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CheckBackupStatusUseCase>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                CHECK_CLOUD_STATUS_WORK,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
        }

        override fun onStop(owner: LifecycleOwner) {
            workManager.cancelUniqueWork(CHECK_CLOUD_STATUS_WORK)
        }
    }

    companion object {
        private const val SYNC_CLOUD_PROFILE_WORK = "sync_cloud_profile"
        private const val CHECK_CLOUD_STATUS_WORK = "check_cloud_status"
        private const val ONE_SECOND_DEBOUNCE = 1000L
    }
}
