package rdx.works.profile.cloudbackup

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rdx.works.core.mapError
import rdx.works.core.then
import rdx.works.profile.BuildConfig
import rdx.works.profile.cloudbackup.model.GoogleAccount
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.coroutines.resume

class GoogleSignInManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun createSignInIntent(): Intent = getGoogleSignInClient(applicationContext).signInIntent

    suspend fun handleSignInResult(result: ActivityResult): Result<GoogleAccount> {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine<Result<GoogleAccount>> { continuation ->
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .addOnSuccessListener { googleSignInAccount ->
                        val email = googleSignInAccount.email
                        if (googleSignInAccount.grantedScopes.contains(driveAppDataScope) && email != null) {
                            val googleAccount = GoogleAccount(
                                email = email,
                                name = googleSignInAccount.displayName,
                                photoUrl = googleSignInAccount.photoUrl
                            )
                            continuation.resumeIfActive(Result.success(value = googleAccount))
                        } else {
                            continuation.resumeIfActive(Result.failure(BackupServiceException.UnauthorizedException))
                        }
                    }
                    .addOnCanceledListener {
                        continuation.resumeIfActive(Result.failure(BackupServiceException.UnauthorizedException))
                    }
                    .addOnFailureListener { error ->
                        val recoverableIntent = (error as? UserRecoverableAuthException)?.intent
                        if (recoverableIntent != null) {
                            continuation.resumeIfActive(
                                Result.failure(
                                    BackupServiceException.RecoverableUnauthorizedException(
                                        recoverableIntent
                                    )
                                )
                            )
                        } else {
                            continuation.resumeIfActive(Result.failure(BackupServiceException.UnauthorizedException))
                        }
                    }
            }
        }.then { googleAccount ->
            ensureAccessToDrive(googleAccount)
        }.onFailure {
            if (it is BackupServiceException.UnauthorizedException) {
                signOut()
            }
        }
    }

    suspend fun signOut() {
        googleSignOut()
        googleRevokeAccess()
    }

    // IMPORTANT NOTE
    // This method will return the GoogleSignInAccount even if the access has been revoked from Drive settings.
    // We must use the Drive service (e.g. accessing the files) in order to get a UserRecoverableAuthIOException.
    fun getSignedInGoogleAccount(): GoogleAccount? {
        GoogleSignIn.getLastSignedInAccount(applicationContext)?.let { googleSignInAccount ->
            val isDriveAccessGranted = googleSignInAccount.grantedScopes.contains(driveAppDataScope)
            val email = googleSignInAccount.email

            return if (isDriveAccessGranted && email != null) {
                GoogleAccount(
                    email = email,
                    name = googleSignInAccount.account?.name,
                    photoUrl = googleSignInAccount.photoUrl
                )
            } else {
                null
            }
        } ?: return null
    }

    fun isSignedIn() = getSignedInGoogleAccount()?.email.isNullOrEmpty().not()

    fun getDrive(account: GoogleAccount? = getSignedInGoogleAccount()): Drive {
        val email = account?.email
        if (email.isNullOrEmpty()) {
            Timber.tag("CloudBackup").e("☁\uFE0F not signed in")
            throw BackupServiceException.UnauthorizedException
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(DriveScopes.DRIVE_APPDATA)
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
        ).setApplicationName("Radix Wallet").build()
    }

    // In order to confirm that wallet is authorized to access drive files,
    // we must start the Drive service and access the files.
    // If the account (email) is not authorized the function will throw an UserRecoverableAuthIOException.
    // This method is currently used ONLY when user signs in (see handleSignInResult fun)
    //
    // ⚠️ The Drive service might take even seconds to return a result.
    private suspend fun ensureAccessToDrive(account: GoogleAccount): Result<GoogleAccount> = runCatching {
        withContext(ioDispatcher) {
            getDrive(account = account).files()
                .list()
                .setSpaces("appDataFolder")
                .execute()
        }

        account
    }.mapError {
        if (it is UserRecoverableAuthIOException) {
            BackupServiceException.RecoverableUnauthorizedException(it.intent)
        } else {
            BackupServiceException.UnauthorizedException
        }
    }

    private suspend fun googleSignOut() {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                getGoogleSignInClient(applicationContext).signOut()
                    .addOnSuccessListener {
                        continuation.resumeIfActive(Unit)
                    }
                    .addOnCanceledListener {
                        continuation.resumeIfActive(Unit)
                    }
                    .addOnFailureListener {
                        continuation.resumeIfActive(Unit)
                    }
            }
        }
    }

    private suspend fun googleRevokeAccess() {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                getGoogleSignInClient(applicationContext).revokeAccess()
                    .addOnSuccessListener {
                        continuation.resumeIfActive(Unit)
                    }
                    .addOnCanceledListener {
                        continuation.resumeIfActive(Unit)
                    }
                    .addOnFailureListener {
                        continuation.resumeIfActive(Unit)
                    }
            }
        }
    }

    companion object {

        private val driveAppDataScope = Scope(DriveScopes.DRIVE_APPDATA)

        private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(driveAppDataScope)
                .requestEmail()
                .build()
            return GoogleSignIn.getClient(context, signInOptions)
        }
    }
}

private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}
