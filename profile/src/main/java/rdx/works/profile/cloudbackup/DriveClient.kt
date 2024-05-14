package rdx.works.profile.cloudbackup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.toJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rdx.works.core.domain.cloudbackup.GoogleDriveFileId
import rdx.works.profile.BuildConfig
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.backup.CloudBackupFile
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import rdx.works.profile.domain.backup.toCloudBackupProperties
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject

interface DriveClient {

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
    @ApplicationContext private val context: Context,
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

    override suspend fun backupProfile(
        googleDriveFileId: GoogleDriveFileId?,
        profile: Profile
    ): Result<CloudBackupFileEntity> {
        return if (googleDriveFileId == null) { // if true then this is the first attempt to backup the profile!
            createBackupFile(profile = profile)
        } else {
            updateBackupFile(
                googleDriveFileId = googleDriveFileId,
                profile = profile
            )
        }
    }

    override suspend fun fetchCloudBackupFileEntities(): Result<List<CloudBackupFileEntity>> = withContext(ioDispatcher) {
        runCatching {// TODO catch exception? e.g. authorization exception
            getFiles().map { file -> CloudBackupFileEntity(file) }
        }
    }

    override suspend fun downloadCloudBackup(
        entity: CloudBackupFileEntity
    ): Result<CloudBackupFile> = withContext(Dispatchers.IO) {
        getFileContents(entity.id.id).map {
            CloudBackupFile(
                fileEntity = entity,
                serializedProfile = it
            )
        }
    }

    override suspend fun claimCloudBackup(file: CloudBackupFileEntity): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            getDrive().files()
                .copy(
                    file.id.id,
                    file.newFile(claimedByDevice = deviceInfoRepository.getDeviceInfo().displayName)
                )
                .setFields(claimFields)
                .execute()
        }.mapCatching { copiedFile ->
            getDrive().files().delete(file.id.id).execute()

            CloudBackupFileEntity(copiedFile)
        }
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

            getDrive().files()
                .create(backupFile, backupContent)
                .setFields(backupFields)
                .execute().let { file ->
                    CloudBackupFileEntity(file)
                }
        }
    }

    private suspend fun updateBackupFile(
        googleDriveFileId: GoogleDriveFileId,
        profile: Profile
    ): Result<CloudBackupFileEntity> = withContext(ioDispatcher) {
        runCatching {
            val profileSerialized = profile.toJson()
            val backupContent = ByteArrayContent("application/json", profileSerialized.toByteArray())
            getDrive().files()
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
        }
    }

    private suspend fun getFileContents(fileId: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val outputStream = ByteArrayOutputStream()
            getDrive().files().get(fileId)
                .setFields(getFilesFields)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray().toString(Charsets.UTF_8)
        }
    }

    private suspend fun getFiles(): List<File> = withContext(ioDispatcher) {
        getDrive().files()
            .list()
            .setSpaces(APP_DATA_FOLDER)
            .setFields(getFilesFields)
            .execute()
            .files
    }

    private fun getDrive(): Drive {
        val email = googleSignInManager.getSignedInGoogleAccount()?.email

        if (email.isNullOrEmpty()) {
            Timber.tag("CloudBackup").e("☁\uFE0F not signed in")
            throw IOException("not signed in")
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_METADATA)
        ).apply {
            selectedAccountName = email
        }

        return Drive.Builder(
            NetHttpTransport().apply {
                val logger = Logger.getLogger(HttpTransport::class.java.name)
                logger.level = if (BuildConfig.DEBUG) Level.CONFIG else Level.OFF
            },
            GsonFactory.getDefaultInstance(),
            credential
        ).build()
    }

    companion object {
        private const val APP_DATA_FOLDER = "appDataFolder"
    }

    /*
    private fun printException(exception: Exception) {
        when (exception) {
            is GoogleJsonResponseException -> GoogleDriveError.HttpApiFailure
            is GooglePlayServicesAvailabilityException -> GoogleDriveError.PlayServicesUnavailable
            is UserRecoverableAuthException -> GoogleDriveError.UserPermissionDenied
            is UserRecoverableAuthIOException -> GoogleDriveError.UserPermissionDenied
            is GoogleAuthException -> GoogleDriveError.AuthFailure
            is IOException -> GoogleDriveError.NetworkUnavailable
            else -> GoogleDriveError.Unknown
        }
    }
     */
}
