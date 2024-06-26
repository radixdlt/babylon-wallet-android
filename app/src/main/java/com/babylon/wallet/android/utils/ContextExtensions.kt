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
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.Browser
import okhttp3.HttpUrl
import timber.log.Timber

val backupSettingsScreenIntent: Intent
    get() = Intent().apply {
        component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.backup.component.BackupSettingsActivity"
        )
    }

fun Context.biometricAuthenticate(
    authenticationCallback: (biometricAuthenticationResult: BiometricAuthenticationResult) -> Unit
) {
    findFragmentActivity()?.let { activity ->
        activity.activityBiometricAuthenticate(
            authenticationCallback = { biometricAuthenticationResult ->
                authenticationCallback(biometricAuthenticationResult)
            }
        )
    }
}

suspend fun Context.biometricAuthenticateSuspend(allowIfDeviceIsNotSecure: Boolean = false): Boolean {
    if (allowIfDeviceIsNotSecure) return true
    return findFragmentActivity()?.biometricAuthenticateSuspend() ?: false
}

fun Context.openUrl(url: HttpUrl, browser: Browser? = null) = openUrl(url.toString(), browser)
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
