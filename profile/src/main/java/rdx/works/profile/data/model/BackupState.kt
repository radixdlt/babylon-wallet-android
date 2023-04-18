package rdx.works.profile.data.model

import android.text.format.DateUtils
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed class BackupState {

    data class Open(
        val lastBackup: Instant?,
        val lastProfileSave: Instant,
        private val lastCheck: Instant
    ): BackupState() {

        val lastBackupTimeRelative: String?
            get() = lastBackup?.let { DateUtils.getRelativeTimeSpanString(it.toEpochMilli()) }?.toString()

        val isWithinWindow: Boolean
            get() {
                if (lastBackup == null) return false

                if (lastProfileSave.epochSecond > lastBackup.epochSecond) return true

                val daysDifference = lastBackup.until(lastProfileSave, ChronoUnit.DAYS)
                return daysDifference < OUTSTANDING_NO_BACKUP_TIME_DAYS
            }

        companion object {
            private const val OUTSTANDING_NO_BACKUP_TIME_DAYS = 3
        }
    }

    object Closed: BackupState()

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
                    lastCheck = Instant.now()
                )
            } else {
                Closed
            }
        }
    }
}
