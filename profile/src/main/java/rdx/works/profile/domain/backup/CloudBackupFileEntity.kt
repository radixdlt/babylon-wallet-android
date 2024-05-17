package rdx.works.profile.domain.backup

import com.google.api.services.drive.model.File
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileId
import com.radixdlt.sargon.Timestamp
import kotlinx.serialization.Serializable
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.sargon.ProfileIdSerializer
import rdx.works.profile.domain.backup.CloudBackupFileEntity.Companion.LAST_MODIFIED_KEY
import rdx.works.profile.domain.backup.CloudBackupFileEntity.Companion.LAST_USED_ON_DEVICE_KEY
import rdx.works.profile.domain.backup.CloudBackupFileEntity.Companion.TOTAL_NUMBER_OF_ACCOUNTS_KEY
import rdx.works.profile.domain.backup.CloudBackupFileEntity.Companion.TOTAL_NUMBER_OF_PERSONAS_KEY
import java.time.format.DateTimeFormatter

@Serializable
data class CloudBackupFileEntity(
    val id: GoogleDriveFileId,
    @Serializable(with = ProfileIdSerializer::class)
    val profileId: ProfileId,
    private val properties: Map<String, String>
) {

    constructor(
        id: GoogleDriveFileId,
        profileId: ProfileId,
        lastUsedOnDeviceName: String,
        lastUsedOnDeviceModified: Timestamp,
        totalNumberOfAccountsOnAllNetworks: Int,
        totalNumberOfPersonasOnAllNetworks: Int
    ) : this(
        id = id,
        profileId = profileId,
        properties = mapOf(
            LAST_USED_ON_DEVICE_KEY to lastUsedOnDeviceName,
            LAST_MODIFIED_KEY to lastUsedOnDeviceModified.format(DateTimeFormatter.ISO_DATE_TIME),
            TOTAL_NUMBER_OF_ACCOUNTS_KEY to totalNumberOfAccountsOnAllNetworks.toString(),
            TOTAL_NUMBER_OF_PERSONAS_KEY to totalNumberOfPersonasOnAllNetworks.toString()
        )
    )

    constructor(
        id: GoogleDriveFileId,
        profile: Profile,
        newDeviceName: String
    ) : this(
        id = id,
        profileId = profile.header.id,
        properties = profile.toCloudBackupProperties(claimedByDevice = newDeviceName)
    )

    constructor(file: File) : this(
        id = GoogleDriveFileId(file.id),
        profileId = ProfileId.fromString(file.name.removeSuffix(".json")),
        properties = file.appProperties
    )

    val lastUsedOnDeviceName: String by lazy {
        properties[LAST_USED_ON_DEVICE_KEY].orEmpty()
    }

    val lastUsedOnDeviceModified: Timestamp by lazy {
        Timestamp.parse(properties.getValue(LAST_MODIFIED_KEY))
    }

    val totalNumberOfAccountsOnAllNetworks: Int by lazy {
        properties[TOTAL_NUMBER_OF_ACCOUNTS_KEY]?.toIntOrNull() ?: 0
    }

    val totalNumberOfPersonasOnAllNetworks: Int by lazy {
        properties[TOTAL_NUMBER_OF_PERSONAS_KEY]?.toIntOrNull() ?: 0
    }

    fun newFile(claimedByDevice: String): File {
        val file = File()
        file.name = "$profileId.json"
        file.appProperties = properties.toMutableMap().apply {
            this[LAST_USED_ON_DEVICE_KEY] = claimedByDevice
        }
        return file
    }

    companion object {
        const val LAST_USED_ON_DEVICE_KEY = "key_last_used_on_device"
        const val LAST_MODIFIED_KEY = "key_last_modified"
        const val TOTAL_NUMBER_OF_ACCOUNTS_KEY = "key_total_number_of_accounts"
        const val TOTAL_NUMBER_OF_PERSONAS_KEY = "key_total_number_of_personas"

        const val LAST_USED_DATE_FORMAT_SHORT_MONTH = "d MMM yyyy"
    }
}

fun Profile.toCloudBackupProperties(claimedByDevice: String = header.lastUsedOnDevice.description): Map<String, String> = mapOf(
    LAST_USED_ON_DEVICE_KEY to claimedByDevice,
    LAST_MODIFIED_KEY to header.lastModified.format(DateTimeFormatter.ISO_DATE_TIME),
    TOTAL_NUMBER_OF_ACCOUNTS_KEY to header.contentHint.numberOfAccountsOnAllNetworksInTotal.toString(),
    TOTAL_NUMBER_OF_PERSONAS_KEY to header.contentHint.numberOfPersonasOnAllNetworksInTotal.toString(),
)
