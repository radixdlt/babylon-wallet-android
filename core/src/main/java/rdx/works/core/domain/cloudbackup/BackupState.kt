package rdx.works.core.domain.cloudbackup

import android.text.format.DateUtils
import com.radixdlt.sargon.Timestamp
import java.time.Instant
import java.time.ZoneId

/**
 * BackupState reflects the state of Backup screen and
 * takes into consideration the cloud backup state and the manual backup state.
 *
 * CloudBackupEnabled and CloudBackupDisabled express the toggle button state. CloudBackupEnabled = on / CloudBackupDisabled = off
 *
 * Important note: CloudBackupEnabled doesn't necessarily means that cloud backup is working properly.
 * Thus, the hasAnyErrors in the CloudBackupEnabled.
 * If this is true the backup screen shows warnings and toggle remains on.
 *
 */
sealed class BackupState {

    // email for cloud backup authorization
    abstract val email: String?
    abstract val lastCloudBackupTime: Timestamp?
    abstract val lastManualBackupTime: Instant?
    abstract val lastModifiedProfileTime: Timestamp?

    data class CloudBackupEnabled(
        override val email: String,
        val hasAnyErrors: Boolean = false,
        override val lastCloudBackupTime: Timestamp? = null,
        override val lastManualBackupTime: Instant? = null,
        override val lastModifiedProfileTime: Timestamp? = null,
    ) : BackupState()

    data class CloudBackupDisabled(
        override val email: String?,
        override val lastCloudBackupTime: Timestamp?,
        override val lastManualBackupTime: Instant?,
        override val lastModifiedProfileTime: Timestamp?
    ) : BackupState()

    // It is used for the login status of the Configuration Backup screen.
    // Cloud backup might be authorized even if state is disabled.
    val isAuthorized: Boolean
        get() = email.isNullOrEmpty().not()

    // it is needed to inform the user when the last cloud backup happened
    val lastCloudBackupLabel: String?
        get() = lastCloudBackupTime?.toInstant()?.toEpochMilli()?.let { epochMilli ->
            DateUtils.getRelativeTimeSpanString(epochMilli)
        }?.toString()

    // it is also needed to inform the user when the last manual backup happened
    val lastManualBackupLabel: String?
        get() = lastManualBackupTime?.toEpochMilli()?.let { epochMilli ->
            DateUtils.getRelativeTimeSpanString(epochMilli)
        }?.toString()

    val isCloudBackupEnabled: Boolean
        get() = this is CloudBackupEnabled

    val isCloudBackupSynced: Boolean
        get() = if (lastModifiedProfileTime != null && lastCloudBackupTime != null) {
            requireNotNull(lastModifiedProfileTime).isBefore(lastCloudBackupTime)
        } else {
            false
        }

    val isManualBackupSynced: Boolean
        get() = if (lastModifiedProfileTime != null && lastManualBackupTime != null) {
            requireNotNull(lastModifiedProfileTime).toInstant().isBefore(lastManualBackupTime)
        } else {
            false
        }

    val isCloudBackupNotUpdated: Boolean
        get() {
            return when (this) {
                is CloudBackupDisabled -> {
                    true
                }
                is CloudBackupEnabled -> {
                    hasAnyErrors
                }
            }
        }

    // neither an updated cloud backup nor an updated manual backup
    val isNotUpdated: Boolean
        get() = backupWarning != null

    // warnings in Backup screen take into consideration the cloud backup state AND the manual backup state
    val backupWarning: BackupWarning?
        get() = when (this) {
            is CloudBackupDisabled -> {
                val lastManualBackupTimestamp = lastManualBackupTime?.let {
                    Timestamp.ofInstant(it, ZoneId.systemDefault())
                }

                if (lastManualBackupTimestamp == null) {
                    BackupWarning.CLOUD_BACKUP_DISABLED_WITH_NO_MANUAL_BACKUP
                } else if (lastManualBackupTimestamp.isBefore(lastModifiedProfileTime)) {
                    BackupWarning.CLOUD_BACKUP_DISABLED_WITH_OUTDATED_MANUAL_BACKUP
                } else {
                    null // here is the case where we have an updated manual backup file so we don't need to show warning
                }
            }

            is CloudBackupEnabled -> {
                if (hasAnyErrors) {
                    BackupWarning.CLOUD_BACKUP_SERVICE_ERROR
                } else {
                    null
                }
            }
        }
}

// refer to this table
// https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3392569357/Security-related+Problem+States+in+the+Wallet
enum class BackupWarning {
    CLOUD_BACKUP_SERVICE_ERROR,
    CLOUD_BACKUP_DISABLED_WITH_NO_MANUAL_BACKUP,
    CLOUD_BACKUP_DISABLED_WITH_OUTDATED_MANUAL_BACKUP
}
