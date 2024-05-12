package rdx.works.profile.cloudbackup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.cloudbackup.model.CloudFileMetadata
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

interface DriveClient {

    suspend fun updateBackupProfile(
        encodedProfile: String,
        fileMetadata: CloudFileMetadata
    ): Result<File>

    /**
     * It fetches a list of available files with their metadata.
     * It does not download the actual profiles.
     */
    suspend fun fetchBackedUpProfilesMetadata(): List<File>

    /**
     * It downloads the actual profile given a fileId.
     */
    suspend fun fetchBackedUpProfile(fileId: String): String?
}

class DriveClientImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val googleSignInManager: GoogleSignInManager
) : DriveClient {

    private val updateFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "appProperties"
    ).joinToString(separator = ",")

    private val fetchFields = listOf(
        "id",
        "name",
        "modifiedTime",
        "fullFileExtension",
        "mimeType",
        "size",
        "appProperties"
    ).joinToString(prefix = "files(", postfix = ")", separator = ",")

    override suspend fun updateBackupProfile(
        encodedProfile: String,
        fileMetadata: CloudFileMetadata
    ): Result<File> = withContext(ioDispatcher) {
        if (getFiles().isEmpty()) {
            Timber.d("☁\uFE0F no backup file. Create a new one")
            createProfileEntry(
                encodedProfile = encodedProfile,
                cloudFileMetadata = fileMetadata
            )
        } else {
            runCatching {
                val backupFile = getFiles().firstOrNull { file ->
                    val name = file.name.removeSuffix(".json")
                    name == fileMetadata.profileId.toString()
                }

                if (backupFile == null) {
                    Timber.d("☁\uFE0F backup file with name ${fileMetadata.profileId} not found")
                    return@withContext Result.failure(IOException("backup file not found"))
                }
                val bytes = ByteArrayContent("application/json", encodedProfile.toByteArray())

                backupFile.setAppProperties(fileMetadata.toMap())
                getDrive().files()
                    .update(backupFile.id, null, bytes)
                    .setFields(updateFields)
                    .execute()
                    .also { file ->
                        Timber.d("☁\uFE0F backup file with name ${file.name} found and fileId: ${file.id}")
                    }
            }
        }
    }

    override suspend fun fetchBackedUpProfilesMetadata(): List<File> = withContext(ioDispatcher) {
        // TODO catch exception? e.g. authorization exception
        val allFiles = getFiles()
        Timber.d("☁\uFE0F fetched backed up profiles: ${allFiles.size}")
        allFiles.also { files ->
            files.map { file ->
                Timber.d("      ☁\uFE0F profile: ${file.name} with fileId: ${file.id}")
            }
        }
    }

    override suspend fun fetchBackedUpProfile(fileId: String): String? {
        // TODO catch exception? e.g. authorization exception
        val profile = getFileContents(fileId).getOrNull()?.let {
            Timber.d("☁\uFE0F fetched backup profile with fileId: $fileId successfully! Size = ${it.length}")
            it
        }
        return profile
    }

    private suspend fun createProfileEntry(
        encodedProfile: String,
        cloudFileMetadata: CloudFileMetadata
    ): Result<File> = withContext(ioDispatcher) {
        val backUpFileName = cloudFileMetadata.profileId.toString()

        runCatching {
            val metadata = File()
                .setParents(listOf(APP_DATA_FOLDER))
                .setMimeType("application/json")
                .setName("$backUpFileName.json")
                .setAppProperties(cloudFileMetadata.toMap())

            val contentBytes = ByteArrayContent("application/json", encodedProfile.toByteArray())

            getDrive().files()
                .create(metadata, contentBytes)
                .setFields(updateFields)
                .execute()
                .also { file ->
                    Timber.d("☁\uFE0F profile entry created successfully with name ${file.name} and fileId: ${file.id}")
                }
        }
    }

    private suspend fun getFiles(): List<File> = withContext(ioDispatcher) {
        getDrive().files()
            .list()
            .setSpaces(APP_DATA_FOLDER)
            .setFields(fetchFields)
            .execute()
            .files
    }

    private suspend fun getFileContents(id: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val outputStream = ByteArrayOutputStream()
            getDrive().files().get(id)
                .setFields(updateFields)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray().toString(Charsets.UTF_8)
        }
    }

    private fun getDrive(): Drive {
        val email = googleSignInManager.getSignedInGoogleAccount()?.email

        if (email.isNullOrEmpty()) {
            Timber.e("☁\uFE0F not signed in")
            throw IOException("not signed in")
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccountName = email
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("get a name")
            .build()
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

fun String.toUUIDorNull() = runCatching { UUID.fromString(this) }.getOrNull()

fun File.deviceID() = name.removeSuffix(".json").toUUIDorNull()
