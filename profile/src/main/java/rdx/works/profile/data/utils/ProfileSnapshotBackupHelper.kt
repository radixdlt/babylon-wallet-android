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
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.ProfileRepository
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.Date

class ProfileSnapshotBackupHelper(context: Context) : BackupHelper {

    private val backupProfileRepository: BackupProfileRepository

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupHelperEntryPoint::class.java
        )
        backupProfileRepository = entryPoint.backupProfileRepository()
    }

    override fun performBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        val snapshotSerialised = runBlocking {
            backupProfileRepository.getSnapshotForCloudBackup()
        }
        log("Backup started for snapshot: $snapshotSerialised")

        data?.apply {
            val byteArray = snapshotSerialised?.toByteArray(Charsets.UTF_8) ?: byteArrayOf()
            val len: Int = byteArray.size

            writeEntityHeader(ENTITY_HEADER, len)
            writeEntityData(byteArray, len)
        }

        FileOutputStream(newState.fileDescriptor).also {
            DataOutputStream(it).writeLong(Date().time)
        }
    }

    override fun restoreEntity(data: BackupDataInputStream) {
        try {
            log("Restoring for key: ${data.key}")
            val byteArray = ByteArray(data.size())
            data.read(byteArray)
            val snapshot = byteArray.toString(Charsets.UTF_8)

            runBlocking {
                val isRestored = backupProfileRepository.saveRestoringSnapshotFromCloud(snapshot)

                if (isRestored) {
                    log("Saved restored profile")
                } else {
                    log("Restored profile discarded or incompatible")
                }
            }
        } catch (exception: Exception) {
            log(exception)
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

    @SuppressLint("LogNotTimber")
    private fun log(exception: Exception) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, exception)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupHelperEntryPoint {
        fun backupProfileRepository(): BackupProfileRepository
    }

    companion object {
        private const val TAG = "Backup"
        private const val ENTITY_HEADER = "snapshot"
    }
}
