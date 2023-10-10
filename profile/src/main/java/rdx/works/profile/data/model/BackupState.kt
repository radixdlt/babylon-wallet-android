package rdx.works.profile.data.model

import android.text.format.DateUtils
import java.time.Duration
import java.time.Instant

sealed class BackupState {

    data class Open(
        private val lastCloudBackupTime: Instant?,
        private val lastProfileUpdate: Instant,
        private val lastCheck: Instant
    ) : BackupState() {

        val lastBackupTimeRelative: String?
            get() = lastCloudBackupTime?.let { DateUtils.getRelativeTimeSpanString(it.toEpochMilli()) }?.toString()

        val isWithinWindow: Boolean
            get() {
                if (lastCloudBackupTime == null) return false

                if (lastProfileUpdate.epochSecond < lastCloudBackupTime.epochSecond) return true

                val duration = Duration.between(lastCloudBackupTime, lastProfileUpdate)
                return duration.toDays() < OUTSTANDING_NO_BACKUP_TIME_DAYS
            }

        companion object {
            private const val OUTSTANDING_NO_BACKUP_TIME_DAYS = 3
        }
    }

    data object Closed : BackupState()

    val isWarningVisible: Boolean
        get() = this is Closed || (this is Open && !isWithinWindow)

}
