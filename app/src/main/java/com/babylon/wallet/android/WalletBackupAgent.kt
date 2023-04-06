package com.babylon.wallet.android

import android.annotation.SuppressLint
import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import rdx.works.profile.datastore.EncryptedPreferencesManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.Date

class WalletBackupAgent: BackupAgent() {

    private lateinit var preferencesManager: EncryptedPreferencesManager

    override fun onCreate() {
        super.onCreate()

        val entryPoint = EntryPointAccessors.fromApplication(this.applicationContext, ExampleContentProviderEntryPoint::class.java)
        preferencesManager = entryPoint.encryptedPreferencesManager()
    }

    @SuppressLint("LogNotTimber")
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor) {
        Log.d("Backup", "Backup started")
        val snapshot = runBlocking {
            preferencesManager.encryptedProfile.firstOrNull()
        }

        Log.d("Backup", "Snapshot read")

        if (snapshot != null) {
            val buffer: ByteArray = ByteArrayOutputStream().run {
                DataOutputStream(this).apply {
                    writeUTF(snapshot)
                }
                toByteArray()
            }
            val len: Int = buffer.size
            data?.apply {
                writeEntityHeader("profile-snapshot", len)
                writeEntityData(buffer, len)
            }

            Log.d("Backup", "Backup started for snapshot $snapshot")
            FileOutputStream(newState.fileDescriptor).also {
                DataOutputStream(it).apply {
                    writeLong(Date().time - 6_000_000)
                }
            }
        }
    }

    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor?) {
        var snapshot = ""
        while (data.readNextHeader()) {
            when(data.key) {
                "profile-snapshot" -> {
                    Log.d("Backup", "Entity found")
                    try {
                        val dataBuf = ByteArray(data.dataSize).also {
                            data.readEntityData(it, 0, data.dataSize)
                        }
                        Log.d("Backup", "Byte array read")
                        ByteArrayInputStream(dataBuf).also {
                            DataInputStream(it).apply {
                                snapshot = readUTF()
                            }
                        }

                        runBlocking {
                            preferencesManager.putProfileSnapshot(snapshot)
                        }
                        Log.d("Backup", "Restored $snapshot")
                    } catch (exception: Exception) {
                        Log.w("Backup", exception)
                    }
                }
                else -> data.skipEntityData()
            }
        }

        // Finally, write to the state blob (newState) that describes the restored data
        newState?.let { new ->
            FileOutputStream(new.fileDescriptor).also {
                DataOutputStream(it).apply {
                    writeLong(Date().time)
                }
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExampleContentProviderEntryPoint {
        fun encryptedPreferencesManager(): EncryptedPreferencesManager
    }

}
