package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: DataStoreManager
) : ViewModel() {

    fun setShowOnboarding(showOnboarding: Boolean) {
        viewModelScope.launch {
            preferencesManager.setShowOnboarding(showOnboarding)
        }
    }
}
