package com.babylon.wallet.android.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun FragmentActivity.biometricAuthenticate(
    authenticate: Boolean,
    authenticationCallback: (successful: Boolean) -> Unit,
) {
    if (!authenticate) return

    val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            authenticationCallback(true)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            authenticationCallback(false)
        }

        override fun onAuthenticationFailed() {
            authenticationCallback(false)
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.biometrics_prompt_title))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    val biometricPrompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        authCallback
    )

    biometricPrompt.authenticate(promptInfo)
}

suspend fun FragmentActivity.biometricAuthenticateSuspend(
    authenticate: Boolean
): Boolean {
    return withContext(Dispatchers.Main) {
        suspendCoroutine {
            if (!authenticate) it.resume(false)

            val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    it.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    it.resume(false)
                }

                override fun onAuthenticationFailed() {
                    it.resume(false)
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometrics_prompt_title))
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

            val biometricPrompt = BiometricPrompt(
                this@biometricAuthenticateSuspend,
                ContextCompat.getMainExecutor(this@biometricAuthenticateSuspend),
                authCallback
            )

            biometricPrompt.authenticate(promptInfo)
        }
    }
}

private const val ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL
