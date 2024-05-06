package rdx.works.profile.cloudbackup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rdx.works.profile.di.coroutines.IoDispatcher
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

class GoogleSignInManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    val isCloudBackupAuthorized = getSignedInGoogleAccount()?.email.isNullOrEmpty().not()

    suspend fun createSignInIntent(): Intent {
        return withContext(ioDispatcher) {
            val client = getGoogleSignInClient(applicationContext)
            client.signInIntent
        }
    }

    suspend fun handleSignInResult(result: ActivityResult): Result<GoogleAccount> {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine<Result<GoogleAccount>> { continuation ->

                if (result.resultCode != Activity.RESULT_OK) {
                    continuation.resumeIfActive(Result.failure(exception = getCancelReason(result.data)))
                }

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
                            continuation.resumeIfActive(Result.failure(exception = SecurityException("PermissionNotGranted")))
                        }
                    }
                    .addOnCanceledListener {
                        continuation.resumeIfActive(Result.failure(exception = getCancelReason(result.data)))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeIfActive(Result.failure(exception = exception))
                    }
            }.also { result ->
                if (result.isFailure && result.exceptionOrNull() is SecurityException) {
                    // user signed in but didn't grant access to Drive therefore sign out
                    signOut()
                }
            }
        }.mapCatching { googleAccount ->
            ensureGoogleAccountAuthorizationToDrive(googleAccount.email)
            googleAccount
        }
    }

    suspend fun signOut() {
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

    suspend fun revokeAccess() {
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

    private fun getCancelReason(resultData: Intent?) =
        try {
            val statusCode = resultData?.getParcelableExtra<Status>("googleSignInStatus")?.statusCode
            if (statusCode == NETWORK_ERROR) {
                IOException("network error")
            } else {
                CancellationException("something went wrong")
            }
        } catch (e: Exception) {
            Timber.e("Google sign in failed with reason: ${e.message}")
            CancellationException("something went wrong")
        }

    // In order to confirm that wallet is authorized to access drive files,
    // we must start the Drive service and access the the files.
    // If the account (email) is not authorized the function will throw an UserRecoverableAuthIOException.
    // This method is currently used ONLY when user signs in (see handleSignInResult fun)
    //
    // ⚠️ The Drive service might take even seconds to return a result.
    private suspend fun ensureGoogleAccountAuthorizationToDrive(email: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccountName = email
        }

        val drive = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("get a name")
            .build()

        withContext(ioDispatcher) {
            drive.files()
                .list()
                .setSpaces("appDataFolder")
                .execute()
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
