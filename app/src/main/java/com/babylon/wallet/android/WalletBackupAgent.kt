package com.babylon.wallet.android

import android.app.backup.BackupAgentHelper
import rdx.works.profile.data.utils.ProfileSnapshotBackupHelper

@Deprecated("New cloud backup system (Drive) in place. It is only used to fetch profile from old backup system.")
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
