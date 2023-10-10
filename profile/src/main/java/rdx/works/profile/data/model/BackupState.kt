package rdx.works.profile.data.model

import android.text.format.DateUtils
import rdx.works.core.InstantGenerator
import java.time.Duration
import java.time.Instant

sealed class BackupState {

    data class Open(
        val lastBackup: Instant?,
        val lastProfileSave: Instant,
        private val lastCheck: Instant
    ) : BackupState() {

        val lastBackupTimeRelative: String?
            get() = lastBackup?.let { DateUtils.getRelativeTimeSpanString(it.toEpochMilli()) }?.toString()

        val isWithinWindow: Boolean
            get() {
                if (lastBackup == null) return false

                return lastProfileSave.epochSecond <= lastBackup.epochSecond
            }
    }

    data object Closed : BackupState()

    val isWarningVisible: Boolean
        get() = this is Closed || (this is Open && !isWithinWindow)

    companion object {
        fun from(
            profile: Profile,
            lastBackupInstant: Instant?
        ): BackupState {
            return if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
                Open(
                    lastBackup = lastBackupInstant,
                    lastProfileSave = profile.header.lastModified,
                    lastCheck = InstantGenerator()
                )
            } else {
                Closed
            }
        }
    }
}
