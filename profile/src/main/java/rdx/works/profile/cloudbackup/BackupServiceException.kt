package rdx.works.profile.cloudbackup

import android.content.Intent
import com.radixdlt.sargon.Timestamp
import rdx.works.profile.domain.backup.CloudBackupFileEntity

sealed class BackupServiceException : Exception() {
    data class ServiceException(val statusCode: Int, override val message: String) : BackupServiceException()

    data class ClaimedByAnotherDevice(
        val fileEntity: CloudBackupFileEntity,
        val claimedProfileModifiedTime: Timestamp
    ) : BackupServiceException()

    data object UnauthorizedException : BackupServiceException()

    data class RecoverableUnauthorizedException(val recoverIntent: Intent) : BackupServiceException()

    data class Unknown(override val cause: Throwable) : BackupServiceException()
}