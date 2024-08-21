package com.babylon.wallet.android.utils

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rdx.works.core.logNonFatalException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun FragmentActivity.activityBiometricAuthenticate(
    authenticationCallback: (biometricAuthenticationResult: BiometricAuthenticationResult) -> Unit,
) {
    val biometricManager = BiometricManager.from(this)
    val canAuthenticate = biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    if (!canAuthenticate) {
        logNonFatalException(IllegalStateException("Biometric authentication error. Allowed authenticator types condition not met."))
        authenticationCallback(BiometricAuthenticationResult.Error)
        return
    }

    val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            authenticationCallback(BiometricAuthenticationResult.Succeeded)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            logNonFatalException(IllegalStateException("Biometric authentication error. Code: $errorCode Message: $errString"))
            authenticationCallback(BiometricAuthenticationResult.Error)
        }

        override fun onAuthenticationFailed() {
            authenticationCallback(BiometricAuthenticationResult.Failed)
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.biometrics_prompt_title))
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()

    val biometricPrompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        authCallback
    )

    biometricPrompt.authenticate(promptInfo)
}

suspend fun FragmentActivity.biometricAuthenticateSuspend(): Boolean {
    return withContext(Dispatchers.Main) {
        suspendCoroutine {
            val biometricManager = BiometricManager.from(this@biometricAuthenticateSuspend)
            val canAuthenticate = biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
            if (!canAuthenticate) {
                logNonFatalException(
                    IllegalStateException("Biometric authentication error (suspend). Allowed authenticator types condition not met.")
                )
                it.resume(false)
                return@suspendCoroutine
            }
            /**
             * onAuthenticationFailed is omitted intentionally as it's invoked whenever the presented biometric is not recognized.
             * We must wait for the biometric session to complete.
             */
            val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    it.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    logNonFatalException(
                        IllegalStateException("Biometric authentication error (suspend). Code: $errorCode Message: $errString")
                    )
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

enum class BiometricAuthenticationResult {
    Succeeded, Error, Failed;

    fun biometricsComplete(): Boolean = this == Succeeded || this == Error
}

private val ALLOWED_AUTHENTICATORS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
} else {
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
}
