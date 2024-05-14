package rdx.works.profile.domain.backup

import rdx.works.profile.cloudbackup.DriveClient
import javax.inject.Inject

class DownloadBackedUpProfileFromCloud @Inject constructor(
    private val driveClient: DriveClient
) {

    suspend operator fun invoke(
        entity: CloudBackupFileEntity
    ): Result<CloudBackupFile> {
        return driveClient.downloadCloudBackup(entity = entity)
    }
}
