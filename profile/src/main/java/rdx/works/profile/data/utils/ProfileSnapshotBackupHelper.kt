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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import rdx.works.profile.datastore.EncryptedPreferencesManager
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.Date

class ProfileSnapshotBackupHelper(context: Context) : BackupHelper {

    private val preferencesManager: EncryptedPreferencesManager

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ExampleContentProviderEntryPoint::class.java
        )
        preferencesManager = entryPoint.encryptedPreferencesManager()
    }

    @SuppressLint("LogNotTimber")
    override fun performBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        Log.d("Backup", "Backup started")
        val snapshot = runBlocking {
            preferencesManager.encryptedProfile.firstOrNull()
        }

        Log.d("Backup", "Snapshot read")
        if (snapshot != null) {
            data?.apply {
                val byteArray = snapshot.toByteArray(Charsets.UTF_8)
                val len: Int = byteArray.size

                writeEntityHeader("snapshot", len)
                writeEntityData(byteArray, len)
            }

            Log.d("Backup", "Backup started for snapshot $snapshot")
            FileOutputStream(newState.fileDescriptor).also {
                DataOutputStream(it).writeLong(Date().time) // TODO Change that based on the last backup saved on snapshot
            }
        }
    }

    @SuppressLint("LogNotTimber")
    override fun restoreEntity(data: BackupDataInputStream) {
        try {
            Log.d("Backup", "Restoring ${data.key}")
            val byteArray = ByteArray(data.size())
            data.read(byteArray)
            val snapshot = byteArray.toString(Charsets.UTF_8)

            runBlocking {
                preferencesManager.putProfileSnapshot(snapshot)
            }
            Log.d("Backup", "Restored $snapshot")
        } catch (exception: Exception) {
            Log.w("Backup", exception)
        }
    }

    // TODO Check that
    override fun writeNewStateDescription(newState: ParcelFileDescriptor?) {
//        newState?.let { new ->
//            FileOutputStream(new.fileDescriptor).also {
//                DataOutputStream(it).apply {
//                    writeLong(Date().time)
//                }
//            }
//        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExampleContentProviderEntryPoint {
        fun encryptedPreferencesManager(): EncryptedPreferencesManager
    }
}
