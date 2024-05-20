package rdx.works.core.domain.cloudbackup

import com.radixdlt.sargon.Header
import com.radixdlt.sargon.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.serializers.TimestampSerializer

/**
 * Event stored in local storage, whenever a backup or claim event is successfully occurred.
 */
@Serializable
data class LastCloudBackupEvent(
    /**
     * The file ID stored in Google Drive
     */
    @SerialName("file_id")
    val fileId: GoogleDriveFileId,

    /**
     * The last modified time of the profile that was backed up, as taken from [Header.lastModified]
     */
    @Serializable(with = TimestampSerializer::class)
    @SerialName("profile_modified_time")
    val profileModifiedTime: Timestamp,

    /**
     * The timestamp which the file was successfully backed up in Google Drive.
     */
    @Serializable(with = TimestampSerializer::class)
    @SerialName("cloud_backup_time")
    val cloudBackupTime: Timestamp
)
