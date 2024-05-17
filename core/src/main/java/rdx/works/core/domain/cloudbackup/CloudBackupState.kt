package rdx.works.core.domain.cloudbackup

import android.text.format.DateUtils
import com.radixdlt.sargon.Timestamp

sealed class CloudBackupState {

    // email for cloud backup authorization
    abstract val email: String?

    data class Enabled(override val email: String) : CloudBackupState()

    data class Disabled(
        override val email: String?,
        private val lastCloudBackupTime: Timestamp?
    ) : CloudBackupState() {

        val lastBackup: String?
            get() = lastCloudBackupTime?.toInstant()?.toEpochMilli()?.let { epochMilli ->
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
