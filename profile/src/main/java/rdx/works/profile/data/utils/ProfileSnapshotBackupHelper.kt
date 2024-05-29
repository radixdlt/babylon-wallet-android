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
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.domain.backup.SaveTemporaryRestoringSnapshotUseCase

@Deprecated("New cloud backup system (Drive) in place. It is only used to fetch profile from old backup system.")
class ProfileSnapshotBackupHelper(context: Context) : BackupHelper {

    private val saveTemporaryRestoringSnapshotUseCase: SaveTemporaryRestoringSnapshotUseCase
    private val checkMigrationToNewBackupSystemUseCase: CheckMigrationToNewBackupSystemUseCase

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupHelperEntryPoint::class.java
        )
        saveTemporaryRestoringSnapshotUseCase = entryPoint.saveTemporaryRestoringSnapshotUseCase()
        checkMigrationToNewBackupSystemUseCase = entryPoint.checkMigrationToNewBackupSystemUseCase()
    }

    override fun performBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        runBlocking {
            if (!checkMigrationToNewBackupSystemUseCase()) {
                // Delete old data
                data?.writeEntityHeader("snapshot", 0)
                data?.writeEntityData(byteArrayOf(), 0)
            }
        }
    }

    override fun restoreEntity(data: BackupDataInputStream) {
        log("Restoring for key: ${data.key}")
        runBlocking {
            saveTemporaryRestoringSnapshotUseCase.forDeprecatedCloud(data).onSuccess {
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
        fun saveTemporaryRestoringSnapshotUseCase(): SaveTemporaryRestoringSnapshotUseCase

        fun checkMigrationToNewBackupSystemUseCase(): CheckMigrationToNewBackupSystemUseCase
    }

    companion object {
        private const val TAG = "Backup"
    }
}
