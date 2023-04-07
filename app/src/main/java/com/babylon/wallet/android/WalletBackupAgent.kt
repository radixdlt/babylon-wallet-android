package com.babylon.wallet.android

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import com.babylon.wallet.android.di.ApplicationModule
import rdx.works.profile.data.utils.ProfileSnapshotBackupHelper

class WalletBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        super.onCreate()

        ProfileSnapshotBackupHelper(context = this).also {
            addHelper(BACKUP_KEY_SNAPSHOT, it)
        }
        FileBackupHelper(this, "datastore/${ApplicationModule.PREFERENCES_NAME}.preferences_pb").also {
            addHelper(BACKUP_KEY_PREFERENCES, it)
        }
    }

    companion object {
        private const val BACKUP_KEY_SNAPSHOT = "backup_profile_snapshot"
        private const val BACKUP_KEY_PREFERENCES = "backup_preferences"
    }

}
