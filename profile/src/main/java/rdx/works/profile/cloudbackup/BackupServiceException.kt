package rdx.works.profile.cloudbackup

sealed class BackupServiceException: Exception() {
    data class ServiceException(val statusCode: Int, override val message: String): BackupServiceException()

    data object ProfileClaimedByAnotherDeviceException: BackupServiceException()

    data object UnauthorizedException: BackupServiceException()

    data class Unknown(override val cause: Throwable): BackupServiceException()
}