package com.babylon.wallet.android

import android.content.SharedPreferences

class PreferencesManager(
    private val sharedPreferences: SharedPreferences
) {

    var showOnboarding: Boolean
        get() = sharedPreferences.getBoolean(SHOW_ONBOARDING, true)
        set(value) = sharedPreferences.edit().putBoolean(SHOW_ONBOARDING, value).apply()

    companion object {
        private const val SHOW_ONBOARDING = "show_onboarding"
    }
}
