package com.babylon.wallet.android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.cloudbackup.model.GoogleAccount
import java.io.IOException

interface CanSignInToGoogle {

    fun signInManager(): GoogleSignInManager

    fun onSignInResult(result: Result<GoogleAccount>)
}

@Composable
fun rememberLauncherForSignInToGoogle(
    viewModel: CanSignInToGoogle
): ManagedActivityResultLauncher<Unit, ActivityResult> {
    val coroutineScope = rememberCoroutineScope()

    val recoverSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        coroutineScope.launch {
            val account = viewModel.signInManager().getSignedInGoogleAccount()
            val result = if (activityResult.resultCode == Activity.RESULT_OK && account != null) {
                Result.success(account)
            } else {
                Result.failure(IOException()) // TODO change error
            }
            viewModel.onSignInResult(result)
        }
    }

    return rememberLauncherForActivityResult(GoogleSignInResultLauncher(viewModel)) { activityResult ->
        coroutineScope.launch {
            viewModel.signInManager().handleSignInResult(activityResult)
                .onSuccess {
                    viewModel.onSignInResult(Result.success(it))
                }.onFailure { error ->
                    if (error is UserRecoverableAuthIOException) {
                        recoverSignInLauncher.launch(error.intent)
                    } else {
                        viewModel.onSignInResult(Result.failure(error))
                    }
                }
        }
    }
}

private class GoogleSignInResultLauncher(
    private val viewModel: CanSignInToGoogle
) : ActivityResultContract<Unit, ActivityResult>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return viewModel.signInManager().createSignInIntent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult = ActivityResult(resultCode, intent)

}
