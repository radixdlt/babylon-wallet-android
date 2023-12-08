package com.babylon.wallet.android.utils

import android.app.KeyguardManager
import android.content.Context
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceSecurityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    fun isDeviceSecure(): Boolean = keyguardManager.isDeviceSecure

    fun isDeviceRooted(): Boolean = RootBeer(context).isRooted
}
