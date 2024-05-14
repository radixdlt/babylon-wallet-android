package rdx.works.profile.domain.backup

import rdx.works.profile.cloudbackup.DriveClient
import javax.inject.Inject

class FetchBackedUpProfilesMetadataFromCloud @Inject constructor(
    private val driveClient: DriveClient
) {

    suspend operator fun invoke(): Result<List<CloudBackupFileEntity>> = driveClient.fetchCloudBackupFileEntities()
}
