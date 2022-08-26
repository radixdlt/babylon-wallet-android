package com.babylon.wallet.android.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.R

fun FragmentActivity.biometricAuthenticate(
    successfulAuthentication: () -> Unit
) {
    val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            successfulAuthentication()
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.biometric_prompt_title))
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
