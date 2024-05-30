package rdx.works.profile.domain.backup

import com.google.api.services.drive.model.File
import com.radixdlt.sargon.ContentHint
import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.ProfileId
import com.radixdlt.sargon.ProfileSnapshotVersion
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sample
import kotlinx.serialization.Serializable
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.sargon.fromVersion
import rdx.works.core.serializers.TimestampSerializer
import rdx.works.profile.cloudbackup.data.DriveClientImpl
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class CloudBackupFileEntity(
    val id: GoogleDriveFileId,
    @Serializable(with = TimestampSerializer::class)
    val lastBackup: Timestamp,
    private val properties: Map<String, String>
) {

    constructor(file: File) : this(
        id = GoogleDriveFileId(file.id),
        lastBackup = Timestamp.ofInstant(Instant.ofEpochMilli(file.modifiedTime.value), ZoneId.systemDefault()),
        properties = file.appProperties
    )

    val header: Header by lazy {
        Header(
            snapshotVersion = ProfileSnapshotVersion.fromVersion(properties.getValue(HEADER_SNAPSHOT_VERSION).toUShort()),
            id = ProfileId.fromString(properties.getValue(HEADER_PROFILE_ID)),
            lastModified = properties.getValue(HEADER_LAST_MODIFIED).asTimestamp(),
            creatingDevice = DeviceInfo(
                id = UUID.fromString(properties.getValue(HEADER_CREATING_DEVICE_ID)),
                date = properties.getValue(HEADER_CREATING_DEVICE_DATE).asTimestamp(),
                description = properties.getValue(HEADER_CREATING_DEVICE_DESCRIPTION),
            ),
            lastUsedOnDevice = DeviceInfo(
                id = UUID.fromString(properties.getValue(HEADER_LAST_USED_ON_DEVICE_ID)),
                date = properties.getValue(HEADER_LAST_USED_ON_DEVICE_DATE).asTimestamp(),
                description = properties.getValue(HEADER_LAST_USED_ON_DEVICE_DESCRIPTION),
            ),
            contentHint = ContentHint(
                numberOfAccountsOnAllNetworksInTotal = properties.getValue(HEADER_CONTENT_HINT_TOTAL_ACCOUNTS).toUShort(),
                numberOfPersonasOnAllNetworksInTotal = properties.getValue(HEADER_CONTENT_HINT_TOTAL_PERSONAS).toUShort(),
                numberOfNetworks = properties.getValue(HEADER_CONTENT_HINT_TOTAL_NETWORKS).toUShort()
            )
        )
    }

    fun claim(header: Header): File = File().apply {
        name = "${header.id}.json"
        appProperties = header.toCloudBackupProperties()
        mimeType = "application/json"
    }

    companion object {
        const val HEADER_SNAPSHOT_VERSION = "snapshotVersion"
        const val HEADER_PROFILE_ID = "profileID"
        const val HEADER_LAST_MODIFIED = "lastModified"
        const val HEADER_CREATING_DEVICE_ID = "creatingDeviceID"
        const val HEADER_CREATING_DEVICE_DATE = "creatingDeviceDate"
        const val HEADER_CREATING_DEVICE_DESCRIPTION = "creatingDeviceDescription"
        const val HEADER_LAST_USED_ON_DEVICE_ID = "lastUsedOnDeviceID"
        const val HEADER_LAST_USED_ON_DEVICE_DATE = "lastUsedOnDeviceDate"
        const val HEADER_LAST_USED_ON_DEVICE_DESCRIPTION = "lastUsedOnDeviceDescription"
        const val HEADER_CONTENT_HINT_TOTAL_ACCOUNTS = "numberOfAccounts"
        const val HEADER_CONTENT_HINT_TOTAL_PERSONAS = "numberOfPersonas"
        const val HEADER_CONTENT_HINT_TOTAL_NETWORKS = "numberOfNetworks"

        const val LAST_USED_DATE_FORMAT_SHORT_MONTH = "d MMM yyyy"

        fun newDriveFile(header: Header) = File().apply {
            parents = listOf(DriveClientImpl.APP_DATA_FOLDER)
            name = "${header.id}.json"
            appProperties = header.toCloudBackupProperties()
            mimeType = "application/json"
        }

        @UsesSampleValues
        val sample: Sample<CloudBackupFileEntity>
            get() = object : Sample<CloudBackupFileEntity> {
                override fun invoke(): CloudBackupFileEntity = CloudBackupFileEntity(
                    id = GoogleDriveFileId(id = "drive_id_1"),
                    lastBackup = TimestampGenerator(),
                    properties = Header.sample().toCloudBackupProperties()
                )

                override fun other(): CloudBackupFileEntity = CloudBackupFileEntity(
                    id = GoogleDriveFileId(id = "drive_id_2"),
                    lastBackup = TimestampGenerator(),
                    properties = Header.sample.other().toCloudBackupProperties()
                )
            }
    }
}

private fun String.asTimestamp() = Timestamp.parse(this)
private fun Timestamp.asStringProperty() = format(DateTimeFormatter.ISO_DATE_TIME)
fun Header.toCloudBackupProperties() = mapOf(
    CloudBackupFileEntity.HEADER_SNAPSHOT_VERSION to snapshotVersion.value.toString(),
    CloudBackupFileEntity.HEADER_PROFILE_ID to id.toString(),
    CloudBackupFileEntity.HEADER_LAST_MODIFIED to lastModified.asStringProperty(),
    CloudBackupFileEntity.HEADER_CREATING_DEVICE_ID to creatingDevice.id.toString(),
    CloudBackupFileEntity.HEADER_CREATING_DEVICE_DATE to creatingDevice.date.asStringProperty(),
    CloudBackupFileEntity.HEADER_CREATING_DEVICE_DESCRIPTION to creatingDevice.description,
    CloudBackupFileEntity.HEADER_LAST_USED_ON_DEVICE_ID to lastUsedOnDevice.id.toString(),
    CloudBackupFileEntity.HEADER_LAST_USED_ON_DEVICE_DATE to lastUsedOnDevice.date.asStringProperty(),
    CloudBackupFileEntity.HEADER_LAST_USED_ON_DEVICE_DESCRIPTION to lastUsedOnDevice.description,
    CloudBackupFileEntity.HEADER_CONTENT_HINT_TOTAL_ACCOUNTS to contentHint.numberOfAccountsOnAllNetworksInTotal.toString(),
    CloudBackupFileEntity.HEADER_CONTENT_HINT_TOTAL_PERSONAS to contentHint.numberOfPersonasOnAllNetworksInTotal.toString(),
    CloudBackupFileEntity.HEADER_CONTENT_HINT_TOTAL_NETWORKS to contentHint.numberOfNetworks.toString()
)
