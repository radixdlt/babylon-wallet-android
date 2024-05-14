package rdx.works.core.domain.cloudbackup

import android.text.format.DateUtils
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed class CloudBackupState {

    // email for cloud backup authorization
    abstract val email: String?

    data class Enabled(override val email: String) : CloudBackupState()

    data class Disabled(
        override val email: String?,
        private val lastCloudBackupTime: Instant?
    ) : CloudBackupState() {

        // TODO fix it
        val lastBackup: String
            get() = lastCloudBackupTime?.toEpochMilli()?.let { epochMilli ->
                DateUtils.getRelativeTimeSpanString(Instant.now().minus(epochMilli, ChronoUnit.SECONDS).toEpochMilli())
            }?.toString() ?: "first"
    }

    val isEnabled: Boolean
        get() = this is Enabled

    val isDisabled: Boolean
        get() = this is Disabled

    // It is used for the login status of the Configuration Backup screen.
    // Cloud backup might be authorized even if state is disabled.
    val isAuthorized: Boolean
        get() = email.isNullOrEmpty().not()

    val hasAnyProblems: Boolean
        get() = isDisabled or isAuthorized.not()

    /*
    data class Open(
        private val lastCloudBackupTime: Instant?,
        private val lastProfileUpdate: Timestamp,
        private val lastCheck: Timestamp
    ) : BackupState() {

        val lastBackupTimeRelative: String?
            get() = lastCloudBackupTime?.let { DateUtils.getRelativeTimeSpanString(it.toEpochMilli()) }?.toString()

        val isWithinWindow: Boolean
            get() {
                if (lastCloudBackupTime == null) return false

                if (lastProfileUpdate.toEpochSecond() < lastCloudBackupTime.epochSecond) return true

                val duration = Duration.between(lastCloudBackupTime, lastProfileUpdate)
                return duration.toDays() < OUTSTANDING_NO_BACKUP_TIME_DAYS
            }

        companion object {
            private const val OUTSTANDING_NO_BACKUP_TIME_DAYS = 3
        }
    }
     */
}
