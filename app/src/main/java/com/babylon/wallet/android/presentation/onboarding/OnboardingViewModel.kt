package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
    private val preferencesManager: DataStoreManager
) : ViewModel() {

    fun setShowOnboarding(showOnboarding: Boolean) {
        viewModelScope.launch(coroutineDispatcher) {
            preferencesManager.setShowOnboarding(showOnboarding)
        }
    }
}
