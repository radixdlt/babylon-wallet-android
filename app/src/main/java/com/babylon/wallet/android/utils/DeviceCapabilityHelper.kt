package com.babylon.wallet.android.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceCapabilityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

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
}
