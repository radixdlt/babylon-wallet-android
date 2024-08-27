package com.babylon.wallet.android.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import com.babylon.wallet.android.BuildConfig
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceCapabilityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    val isDeviceSecure: Boolean
        get() = keyguardManager.isDeviceSecure

    private val makeAndModel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    private val systemVersion: String
        get() = "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"

    val supportEmailTemplate: String
        get() = buildString {
            append("\n\n")
            append("App version: ${BuildConfig.VERSION_NAME}\n")
            append("Device: ${makeAndModel}\n")
            append("System version: ${systemVersion}\n")
        }

    fun isDeviceRooted(): Boolean = RootBeer(context).isRooted
}
