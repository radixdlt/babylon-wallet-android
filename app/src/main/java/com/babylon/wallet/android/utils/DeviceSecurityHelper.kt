package com.babylon.wallet.android.utils

import android.app.KeyguardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class DeviceSecurityHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    fun isDeviceSecure(): Boolean = keyguardManager.isDeviceSecure
}
