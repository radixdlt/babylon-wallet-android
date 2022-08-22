package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun showOnboarding(): Boolean {
        val showOnboarding = preferencesManager.showOnboarding
        if (showOnboarding) {
            preferencesManager.showOnboarding = false
        }
        return showOnboarding
    }
}
