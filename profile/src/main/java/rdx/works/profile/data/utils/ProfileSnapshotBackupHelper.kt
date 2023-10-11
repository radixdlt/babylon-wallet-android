package rdx.works.profile.data.utils

import android.annotation.SuppressLint
import android.app.backup.BackupDataInputStream
import android.app.backup.BackupDataOutput
import android.app.backup.BackupHelper
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import rdx.works.profile.BuildConfig
import rdx.works.profile.domain.backup.BackupProfileToCloudUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.Date

class ProfileSnapshotBackupHelper(context: Context) : BackupHelper {

    private val backupProfileToCloudUseCase: BackupProfileToCloudUseCase
    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupHelperEntryPoint::class.java
        )
        backupProfileToCloudUseCase = entryPoint.backupProfileToCloudUseCase()
        saveTemporaryRestoringSnapshotUseCase = entryPoint.saveTemporaryRestoringSnapshotUseCase()
    }

    override fun performBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        log("Backup started")

        runBlocking {
            backupProfileToCloudUseCase(data, ENTITY_HEADER).onSuccess {
                log("Backup successful")
            }.onFailure {
                log("Backup failed $it")
            }
        }

        FileOutputStream(newState.fileDescriptor).also {
            DataOutputStream(it).writeLong(Date().time)
        }
    }

    override fun restoreEntity(data: BackupDataInputStream) {
        log("Restoring for key: ${data.key}")
        runBlocking {
            saveTemporaryRestoringSnapshotUseCase.forCloud(data).onSuccess {
                log("Saved restored profile")
            }.onFailure { error ->
                log("Restored profile discarded or incompatible: ${error.message}")
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun writeNewStateDescription(newState: ParcelFileDescriptor?) {}

    // Timber will not work here, since this helper is initiated with a sandboxed
    // application context. When a backup operation is started, the system creates the BackupAgent and starts the
    // parent Application class and not the BabylonApplication.
    // Check the warning in https://developer.android.com/guide/topics/data/autobackup#ImplementingBackupAgent
    @SuppressLint("LogNotTimber")
    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupHelperEntryPoint {
        fun backupProfileToCloudUseCase(): BackupProfileToCloudUseCase
        fun saveTemporaryRestoringSnapshotUseCase(): SaveTemporaryRestoringSnapshotUseCase
    }

    companion object {
        private const val TAG = "Backup"
        private const val ENTITY_HEADER = "snapshot"
    }
}
