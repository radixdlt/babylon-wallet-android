package com.babylon.wallet.android

import android.app.backup.BackupAgentHelper
import rdx.works.profile.data.utils.ProfileSnapshotBackupHelper

class WalletBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        super.onCreate()

        ProfileSnapshotBackupHelper(context = this).also {
            addHelper(BACKUP_KEY_SNAPSHOT, it)
        }
    }

    companion object {
        private const val BACKUP_KEY_SNAPSHOT = "backup_profile_snapshot"
    }
}
