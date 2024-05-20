package rdx.works.core.domain.cloudbackup

import android.text.format.DateUtils
import com.radixdlt.sargon.Timestamp
import java.time.Instant

sealed class CloudBackupState {

    // email for cloud backup authorization
    abstract val email: String?

    data class Enabled(override val email: String) : CloudBackupState()

    data class Disabled(
        override val email: String?,
        private val lastCloudBackupTime: Timestamp?,
        private val lastManualBackupTime: Instant?
    ) : CloudBackupState() {

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
    }

    val isEnabled: Boolean
        get() = this is Enabled

    val isDisabled: Boolean
        get() = this is Disabled

    // It is used for the login status of the Configuration Backup screen.
    // Cloud backup might be authorized even if state is disabled.
    val isAuthorized: Boolean
        get() = email.isNullOrEmpty().not()
}
