package rdx.works.profile.cloudbackup

import androidx.work.ListenableWorker

interface DriveBackupExecutor {

    suspend operator fun invoke(worker: ListenableWorker): ListenableWorker.Result

}