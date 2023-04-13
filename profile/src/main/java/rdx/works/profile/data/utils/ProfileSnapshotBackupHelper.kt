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
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.datastore.EncryptedPreferencesManager
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.time.Instant
import java.util.Date

class ProfileSnapshotBackupHelper(context: Context) : BackupHelper {

    private val encryptedPreferencesManager: EncryptedPreferencesManager
    private val preferencesManager: PreferencesManager

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupHelperEntryPoint::class.java
        )
        encryptedPreferencesManager = entryPoint.encryptedPreferencesManager()
        preferencesManager = entryPoint.preferencesManager()
    }

    @SuppressLint("LogNotTimber")
    override fun performBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        Log.d("Backup", "Backup started")
        val snapshotSerialised = runBlocking {
            encryptedPreferencesManager.encryptedProfile.firstOrNull()
        }

        if (snapshotSerialised == null) {
            Log.d("Backup", "No snapshot exists to backup")
            return
        }

        if (!isProfileBackupEnabled(snapshotSerialised)) {
            Log.d("Backup", "Backup is disabled in Profile")
            return
        }

        data?.apply {
            val byteArray = snapshotSerialised.toByteArray(Charsets.UTF_8)
            val len: Int = byteArray.size

            writeEntityHeader("snapshot", len)
            writeEntityData(byteArray, len)
        }

        runBlocking {
            preferencesManager.updateLastBackupInstant(Instant.now())
        }

        Log.d("Backup", "Backup started for snapshot $snapshotSerialised")
        FileOutputStream(newState.fileDescriptor).also {
            DataOutputStream(it).writeLong(Date().time) // TODO Change that based on the last backup saved on snapshot
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
                encryptedPreferencesManager.putProfileSnapshotFromBackup(snapshot)
                // Update the "backup" time, since when restoring the profile, we are ensured
                // about the most recent backup time.
                preferencesManager.updateLastBackupInstant(Instant.now())
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

    private fun isProfileBackupEnabled(serialisedSnapshot: String): Boolean {
        val snapshot = try {
            ProfileSnapshot.fromJson(serialisedSnapshot)
        } catch (exception: Exception) {
            null
        }

        return snapshot?.toProfile()?.appPreferences?.security?.isCloudProfileSyncEnabled ?: false
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupHelperEntryPoint {
        fun encryptedPreferencesManager(): EncryptedPreferencesManager

        fun preferencesManager(): PreferencesManager
    }
}
