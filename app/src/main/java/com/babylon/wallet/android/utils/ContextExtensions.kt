package com.babylon.wallet.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController

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
