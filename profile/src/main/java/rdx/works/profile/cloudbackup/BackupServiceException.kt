package rdx.works.profile.cloudbackup

import android.content.Intent

sealed class BackupServiceException : Exception() {
    data class ServiceException(val statusCode: Int, override val message: String) : BackupServiceException()

    data object CloudBackupNotFoundOrClaimed : BackupServiceException()

    data object UnauthorizedException : BackupServiceException()

    data class RecoverableUnauthorizedException(val recoverIntent: Intent) : BackupServiceException()

    data class Unknown(override val cause: Throwable) : BackupServiceException()
}
