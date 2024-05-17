package com.babylon.wallet.android.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.babylon.wallet.android.BuildConfig
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceCapabilityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

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

    fun isDeviceSecure(): Boolean = keyguardManager.isDeviceSecure

    fun canOpenSystemBackupSettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                backupSettingsScreenIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(backupSettingsScreenIntent, PackageManager.MATCH_ALL)
        }.size > 0
    }

    fun isDeviceRooted(): Boolean = RootBeer(context).isRooted
}
