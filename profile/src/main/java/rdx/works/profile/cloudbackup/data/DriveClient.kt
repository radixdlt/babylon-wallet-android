package rdx.works.profile.cloudbackup.data

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.internal.http.HTTP_FORBIDDEN
import okhttp3.internal.http.HTTP_NOT_FOUND
import okhttp3.internal.http.HTTP_UNAUTHORIZED
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.domain.cloudbackup.LastCloudBackupEvent
import rdx.works.core.mapError
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.then
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.backup.CloudBackupFile
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.toCloudBackupProperties
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

interface DriveClient {

    suspend fun backupProfile(
        profile: Profile
    ): Result<CloudBackupFileEntity>

    suspend fun getCloudBackupEntity(
        fileId: GoogleDriveFileId,
        profile: Profile
    ): Result<CloudBackupFileEntity>

    /**
     * It fetches a list of available files with their metadata.
     * It does not download the actual profiles.
     */
    suspend fun fetchCloudBackupFileEntities(): Result<List<CloudBackupFileEntity>>

    /**
     * It downloads and claims the actual backed up profile given a fileId.
     */
    suspend fun downloadCloudBackup(
        entity: CloudBackupFileEntity
    ): Result<CloudBackupFile>

    suspend fun claimCloudBackup(
        file: CloudBackupFileEntity,
        updatedHeader: Header
    ): Result<CloudBackupFileEntity>
}

class DriveClientImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val googleSignInManager: GoogleSignInManager,
    private val preferencesManager: PreferencesManager,
    private val deviceInfoRepository: DeviceInfoRepository
) : DriveClient {

    private val backupFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "appProperties"
    ).joinToString(separator = ",")

    private val claimFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "appProperties"
    ).joinToString(separator = ",")

    private val getFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "fullFileExtension",
        "mimeType",
        "size",
        "appProperties"
    ).joinToString(separator = ",")

    private val getFilesFields = "files($getFields)"

    override suspend fun backupProfile(
        profile: Profile
    ): Result<CloudBackupFileEntity> {
        val fileId = preferencesManager.lastCloudBackupEvent.firstOrNull()?.fileId

        return if (fileId == null) {
            fetchCloudBackupFileEntities()
                .then { existingBackups ->
                    val existingBackup = existingBackups.find { it.header.id == profile.header.id }
                    // Check if profile with the same id is already backed up
                    if (existingBackup == null) {
                        // Create a new one if none existed.
                        createBackupFile(profile = profile)
                    } else {
                        // Claim the one existed before.
                        // This scenario can play out as follows:
                        // 1. The user creates a profile and backs it up to cloud
                        // 2. The user exports the same profile to file.
                        // 3. Either restores this device or in a new device, imports the exported file
                        // 4. This file has the same profile id, but also exists on cloud. We need to claim it.
                        claimCloudBackup(file = existingBackup, updatedHeader = profile.header)
                    }
                }
        } else {
            updateBackupFile(
                googleDriveFileId = fileId,
                profile = profile
            )
        }.onSuccess { entity ->
            preferencesManager.updateLastCloudBackupEvent(
                LastCloudBackupEvent(
                    fileId = entity.id,
                    profileModifiedTime = profile.header.lastModified,
                    cloudBackupTime = entity.lastBackup
                )
            )
        }
    }

    override suspend fun getCloudBackupEntity(
        fileId: GoogleDriveFileId,
        profile: Profile
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            googleSignInManager
                .getDrive()
                .files()
                .get(fileId.id)
                .setFields(getFields)
                .execute().let { file -> CloudBackupFileEntity(file) }
        }.mapCatching { entity ->
            if (entity.header.lastUsedOnDevice.id != deviceInfoRepository.getDeviceInfo().id) {
                throw BackupServiceException.ClaimedByAnotherDevice(entity)
            } else {
                entity
            }
        }.mapDriveError()
    }

    override suspend fun fetchCloudBackupFileEntities(): Result<List<CloudBackupFileEntity>> = withContext(ioDispatcher) {
        runCatching {
            googleSignInManager.getDrive().files()
                .list()
                .setSpaces(APP_DATA_FOLDER)
                .setFields(getFilesFields)
                .execute()
                .files
                .map { file -> CloudBackupFileEntity(file) }
        }.mapDriveError()
    }

    override suspend fun downloadCloudBackup(
        entity: CloudBackupFileEntity
    ): Result<CloudBackupFile> = withContext(ioDispatcher) {
        getFileContents(entity.id.id).map {
            CloudBackupFile(
                fileEntity = entity,
                serializedProfile = it
            )
        }.mapDriveError()
    }

    override suspend fun claimCloudBackup(
        file: CloudBackupFileEntity,
        updatedHeader: Header
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            Timber.tag("CloudBackup").d("Start claiming process with file id: ${file.id}")
            googleSignInManager.getDrive().files()
                .update(
                    file.id.id,
                    file.claim(header = updatedHeader) // Updates the lastUsedOnDevice.id
                )
                .setFields(claimFields)
                .execute()
        }.mapCatching { copiedFile ->
            CloudBackupFileEntity(copiedFile)
        }.onSuccess { entity ->
            preferencesManager.updateLastCloudBackupEvent(
                LastCloudBackupEvent(
                    fileId = entity.id,
                    profileModifiedTime = updatedHeader.lastModified,
                    cloudBackupTime = entity.lastBackup
                )
            )
        }.mapDriveError()
    }

    private suspend fun createBackupFile(
        profile: Profile
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            Timber.tag("CloudBackup").d("Create backup file")
            val driveFile = CloudBackupFileEntity.newDriveFile(profile.header)
            val backupContent = ByteArrayContent("application/json", profile.toJson().toByteArray())

            googleSignInManager.getDrive().files()
                .create(driveFile, backupContent)
                .setFields(backupFields)
                .execute().let { file ->
                    CloudBackupFileEntity(file)
                }
        }.mapDriveError()
    }

    private suspend fun updateBackupFile(
        googleDriveFileId: GoogleDriveFileId,
        profile: Profile
    ): Result<CloudBackupFileEntity> = getCloudBackupEntity(fileId = googleDriveFileId, profile = profile)
        .mapCatching {
            withContext(ioDispatcher) {
                googleSignInManager.getDrive().files()
                    .update(
                        googleDriveFileId.id,
                        File().apply {
                            appProperties = profile.header.toCloudBackupProperties()
                        },
                        ByteArrayContent("application/json", profile.toJson().toByteArray())
                    )
                    .setFields(backupFields)
                    .execute()
                    .let { file ->
                        CloudBackupFileEntity(file)
                    }
            }
        }.mapDriveError()

    private suspend fun getFileContents(fileId: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val outputStream = ByteArrayOutputStream()
            googleSignInManager.getDrive().files().get(fileId)
                .setFields(getFilesFields)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray().toString(Charsets.UTF_8)
        }.mapDriveError()
    }

    private suspend fun <T> Result<T>.mapDriveError(): Result<T> = mapError { error ->
        val mappedError = when (error) {
            is BackupServiceException -> error
            is GoogleAuthIOException -> BackupServiceException.UnauthorizedException
            is GoogleJsonResponseException -> {
                when (error.details.code) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> BackupServiceException.UnauthorizedException
                    else -> {
                        if (error.details.code == HTTP_NOT_FOUND) {
                            preferencesManager.removeLastCloudBackupEvent()
                        }
                        BackupServiceException.ServiceException(statusCode = error.details.code, message = error.details.message)
                    }
                }
            }

            else -> BackupServiceException.Unknown(cause = error)
        }

        Timber.tag("CloudBackup").w(error, "Mapped to $mappedError")
        mappedError
    }

    companion object {
        const val APP_DATA_FOLDER = "appDataFolder"
    }
}
