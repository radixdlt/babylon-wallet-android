package rdx.works.core.domain.cloudbackup

import android.text.format.DateUtils
import com.radixdlt.sargon.Timestamp
import java.time.Instant

/**
 * CloudBackupState reflects the state of the toggle button in BackupScreen.
 * Enabled = on / Disabled = off
 *
 * Important note: Enabled doesn't necessarily means that cloud backup is working properly.
 * Thus, the hasAnyErrors in the Enabled. If this is true the backup screen shows warnings
 * and toggle remains on.
 */
sealed class CloudBackupState {

    // email for cloud backup authorization
    abstract val email: String?
    abstract val lastCloudBackupTime: Timestamp?
    abstract val lastManualBackupTime: Instant?

    data class Enabled(
        override val email: String,
        val hasAnyErrors: Boolean = false,
        override val lastCloudBackupTime: Timestamp? = null,
        override val lastManualBackupTime: Instant? = null,
    ) : CloudBackupState()

    data class Disabled(
        override val email: String?,
        override val lastCloudBackupTime: Timestamp?,
        override val lastManualBackupTime: Instant?
    ) : CloudBackupState()

    // it is needed to inform the user when the last cloud backup happened
    val lastCloudBackup: String?
        get() = lastCloudBackupTime?.toInstant()?.toEpochMilli()?.let { epochMilli ->
            DateUtils.getRelativeTimeSpanString(epochMilli)
        }?.toString()

    // it is also needed to inform the user when the last manual backup happened
    val lastManualBackup: String?
        get() = lastManualBackupTime?.toEpochMilli()?.let { epochMilli ->
            DateUtils.getRelativeTimeSpanString(epochMilli)
        }?.toString()

    val isEnabled: Boolean
        get() = this is Enabled

    val isWorking: Boolean
        get() = this is Disabled || (this as Enabled).hasAnyErrors

    // It is used for the login status of the Configuration Backup screen.
    // Cloud backup might be authorized even if state is disabled.
    val isAuthorized: Boolean
        get() = email.isNullOrEmpty().not()
}
