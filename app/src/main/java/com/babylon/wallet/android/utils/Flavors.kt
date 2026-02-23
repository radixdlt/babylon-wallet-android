package com.babylon.wallet.android.utils

import com.babylon.wallet.android.BuildConfig

object Flavors {

    private const val LIGHT_FLAVOR_NAME = "light"
    private const val FULL_FLAVOR_NAME = "full"

    fun isLightVersion(): Boolean {
        return BuildConfig.FLAVOR == LIGHT_FLAVOR_NAME
    }

    fun isFullVersion(): Boolean {
        return BuildConfig.FLAVOR == FULL_FLAVOR_NAME
    }
}
