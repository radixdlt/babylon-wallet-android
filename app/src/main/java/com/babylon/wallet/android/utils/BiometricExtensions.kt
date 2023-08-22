package com.babylon.wallet.android.utils

import android.os.Build
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
    authenticationCallback: (successful: Boolean) -> Unit,
) {
    val biometricManager = BiometricManager.from(this)
    val canAuthenticate = biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    if (!canAuthenticate) {
        authenticationCallback(false)
        return
    }

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
                it.resume(false)
                return@suspendCoroutine
            }
            val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    it.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
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

private val ALLOWED_AUTHENTICATORS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
} else {
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
}
