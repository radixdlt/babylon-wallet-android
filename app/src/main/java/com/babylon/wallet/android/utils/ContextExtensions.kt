@file:Suppress("MatchingDeclarationName")

package com.babylon.wallet.android.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.babylon.wallet.android.AppLockStateProvider
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.Browser
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import timber.log.Timber

@EntryPoint
@InstallIn(ActivityComponent::class)
interface BiometricAuthenticationEntryPoint {
    fun provideAppLockStateProvider(): AppLockStateProvider
}

fun Context.biometricAuthenticate(
    authenticationCallback: (biometricAuthenticationResult: BiometricAuthenticationResult) -> Unit
) {
    findFragmentActivity()?.let { activity ->
        val appLockStateProvider = EntryPointAccessors.fromActivity(
            activity = activity,
            entryPoint = BiometricAuthenticationEntryPoint::class.java
        ).provideAppLockStateProvider()
        // It appear that below Android 11 Lock Screen uses new activity when there is no fingerprint registered (e.g. PIN).
        // So we pause app locking to avoid double biometric prompt when advanced lock on and we ask for biometrics from a Wallet,
        // eg. when user wants to create account/sign transaction, app is moved to background and advanced lock is applied
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            appLockStateProvider.pauseLocking()
        }
        activity.activityBiometricAuthenticate(
            authenticationCallback = { biometricAuthenticationResult ->
                if (biometricAuthenticationResult.biometricsComplete()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        appLockStateProvider.resumeLocking()
                    }
                }
                authenticationCallback(biometricAuthenticationResult)
            }
        )
    }
}

suspend fun Context.biometricAuthenticateSuspend(): Boolean {
    val fragmentActivity = findFragmentActivity() ?: return false
    val appLockStateProvider = EntryPointAccessors.fromActivity(
        activity = fragmentActivity,
        entryPoint = BiometricAuthenticationEntryPoint::class.java
    ).provideAppLockStateProvider()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        appLockStateProvider.pauseLocking()
    }
    val result = fragmentActivity.biometricAuthenticateSuspend()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        appLockStateProvider.resumeLocking()
    }
    return result
}

fun Context.openUrl(url: String, browser: Browser? = null) = openUrl(url.toUri(), browser)

fun Context.copyToClipboard(
    label: String,
    value: String,
    // Used only for Android versions < Android 13
    successMessage: String? = null
) {
    getSystemService<android.content.ClipboardManager>()?.let { clipboardManager ->

        val clipData = ClipData.newPlainText(
            label,
            value
        )

        clipboardManager.setPrimaryClip(clipData)

        // From Android 13, the system handles the copy confirmation
        if (successMessage != null && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        }
    }
}

@Suppress("SwallowedException")
fun Context.openUrl(uri: Uri, browser: Browser? = null) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
    }
    val info = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    info.find { resolveInfo ->
        Timber.d("Handling browser: ${resolveInfo.activityInfo.packageName}")
        resolveInfo.activityInfo.packageName == browser?.packageName
    }?.let { resolveInfo ->
        intent.setComponent(ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name))
    }
    try {
        startActivity(intent)
    } catch (activityNotFound: ActivityNotFoundException) {
        Toast.makeText(
            this,
            R.string.addressAction_noWebBrowserInstalled,
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Suppress("SwallowedException")
fun Context.openEmail(recipientAddress: String? = null, subject: String? = null, body: String? = null) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        recipientAddress?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
        subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        body?.let { putExtra(Intent.EXTRA_TEXT, it) }
    }
    try {
        startActivity(intent)
    } catch (activityNotFound: ActivityNotFoundException) {
        Toast.makeText(
            this,
            getString(R.string.no_email_client_installed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun Context.shareText(value: String, title: String? = null) {
    val shareIntent = Intent.createChooser(
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, value)
            type = "text/plain"
        },
        title
    )
    startActivity(shareIntent)
}

@Suppress("SwallowedException")
fun NavController.routeExist(route: String): Boolean {
    return try {
        getBackStackEntry(route)
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

fun Context.setWindowSecure(enabled: Boolean) {
    findFragmentActivity()?.window?.apply {
        if (enabled) {
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
