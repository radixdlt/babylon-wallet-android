package com.babylon.wallet.android.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.babylon.wallet.android.R

val backupSettingsScreenIntent: Intent
    get() = Intent().apply {
        component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.backup.component.BackupSettingsActivity"
        )
    }

fun Context.biometricAuthenticate(authenticationCallback: (successful: Boolean) -> Unit) {
    findFragmentActivity()?.let { activity ->
        activity.biometricAuthenticate { authenticatedSuccessfully ->
            authenticationCallback(authenticatedSuccessfully)
        }
    }
}

suspend fun Context.biometricAuthenticateSuspend(allowIfDeviceIsNotSecure: Boolean = false): Boolean {
    if (allowIfDeviceIsNotSecure) return true
    return findFragmentActivity()?.biometricAuthenticateSuspend() ?: false
}

@Suppress("SwallowedException")
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = url.toUri()
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
