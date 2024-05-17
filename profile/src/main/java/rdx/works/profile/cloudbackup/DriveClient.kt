package rdx.works.profile.cloudbackup

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.internal.http.HTTP_FORBIDDEN
import okhttp3.internal.http.HTTP_NOT_FOUND
import okhttp3.internal.http.HTTP_UNAUTHORIZED
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.core.mapError
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.backup.CloudBackupFile
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.toCloudBackupProperties
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

interface DriveClient {

    val backupErrors: StateFlow<BackupServiceException?>

    suspend fun backupProfile(
        googleDriveFileId: GoogleDriveFileId?,
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
        file: CloudBackupFileEntity
    ): Result<CloudBackupFileEntity>
}

class DriveClientImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val googleSignInManager: GoogleSignInManager,
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
        "appProperties"
    ).joinToString(separator = ",")

    private val getFilesFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "fullFileExtension",
        "mimeType",
        "size",
        "appProperties"
    ).joinToString(prefix = "files(", postfix = ")", separator = ",")

    override val backupErrors: MutableStateFlow<BackupServiceException?> = MutableStateFlow(null)

    override suspend fun backupProfile(
        googleDriveFileId: GoogleDriveFileId?,
        profile: Profile
    ): Result<CloudBackupFileEntity> = if (googleDriveFileId == null) { // if true then this is the first attempt to backup the profile!
        createBackupFile(profile = profile)
    } else {
        updateBackupFile(
            googleDriveFileId = googleDriveFileId,
            profile = profile
        )
    }.onFailure { error ->
        if (error is BackupServiceException) {
            backupErrors.update { error }
        }
    }.onSuccess {
        backupErrors.update { null }
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

    override suspend fun claimCloudBackup(file: CloudBackupFileEntity): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            googleSignInManager.getDrive().files()
                .copy(
                    file.id.id,
                    file.newFile(claimedByDevice = deviceInfoRepository.getDeviceInfo().displayName)
                )
                .setFields(claimFields)
                .execute()
        }.mapCatching { copiedFile ->
            googleSignInManager.getDrive().files().delete(file.id.id).execute()

            CloudBackupFileEntity(copiedFile)
        }.mapDriveError()
    }

    private suspend fun createBackupFile(
        profile: Profile
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        val profileSerialized = profile.toJson()
        val backUpFileName = profile.header.id.toString()

        runCatching {
            val backupFile = File()
                .setParents(listOf(APP_DATA_FOLDER))
                .setMimeType("application/json")
                .setName("$backUpFileName.json")
                .setAppProperties(profile.toCloudBackupProperties())

            val backupContent = ByteArrayContent("application/json", profileSerialized.toByteArray())

            googleSignInManager.getDrive().files()
                .create(backupFile, backupContent)
                .setFields(backupFields)
                .execute().let { file ->
                    CloudBackupFileEntity(file)
                }
        }.mapDriveError()
    }

    private suspend fun updateBackupFile(
        googleDriveFileId: GoogleDriveFileId,
        profile: Profile
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            val profileSerialized = profile.toJson()
            val backupContent = ByteArrayContent("application/json", profileSerialized.toByteArray())
            googleSignInManager.getDrive().files()
                .update(
                    googleDriveFileId.id,
                    File().apply {
                        appProperties = profile.toCloudBackupProperties()
                    },
                    backupContent
                )
                .setFields(backupFields)
                .execute()
                .let { file ->
                    CloudBackupFileEntity(file)
                }
        }.mapDriveError(throwClaimByAnotherDeviceError = true)
    }

    private suspend fun getFileContents(fileId: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val outputStream = ByteArrayOutputStream()
            googleSignInManager.getDrive().files().get(fileId)
                .setFields(getFilesFields)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray().toString(Charsets.UTF_8)
        }.mapDriveError()
    }

    private fun <T> Result<T>.mapDriveError(throwClaimByAnotherDeviceError: Boolean = false): Result<T> = mapError { error ->
        val mappedError = when (error) {
            is BackupServiceException -> error
            is GoogleAuthIOException -> BackupServiceException.UnauthorizedException
            is GoogleJsonResponseException -> {
                when (error.details.code) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> BackupServiceException.UnauthorizedException
                    HTTP_NOT_FOUND -> if (throwClaimByAnotherDeviceError) {
                        BackupServiceException.ProfileClaimedByAnotherDeviceException
                    } else {
                        BackupServiceException.ServiceException(statusCode = error.details.code, message = error.details.message)
                    }

                    else -> BackupServiceException.ServiceException(statusCode = error.details.code, message = error.details.message)
                }
            }

            else -> BackupServiceException.Unknown(cause = error)
        }

        Timber.tag("CloudBackup").w(error, "Mapped to $mappedError")
        mappedError
    }

    companion object {
        private const val APP_DATA_FOLDER = "appDataFolder"
    }
}
