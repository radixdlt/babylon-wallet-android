package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager,
    private val preferencesManager: PreferencesManager
) : ViewModel(), OneOffEventHandler<OnboardingViewModel.OnboardingEvent> by OneOffEventHandlerImpl() {

    init {
        // we need to revoke access to GDrive here in case user kills app on account create screen
        viewModelScope.launch {
            val shouldShowEula = preferencesManager.isEulaAccepted.firstOrNull() == false
            if (!shouldShowEula) {
                googleSignInManager.signOut()
            }
        }
    }

    fun onCreateNewWalletClick() {
        viewModelScope.launch {
            val shouldShowEula = preferencesManager.isEulaAccepted.firstOrNull() == false
            if (shouldShowEula) {
                sendEvent(OnboardingEvent.NavigateToEula)
            } else {
                sendEvent(OnboardingEvent.NavigateToCreateNewWallet(googleSignInManager.isSignedIn()))
            }
        }
    }

    sealed interface OnboardingEvent : OneOffEvent {
        data class NavigateToCreateNewWallet(val isWithCloudBackupEnabled: Boolean) : OnboardingEvent
        data object NavigateToEula : OnboardingEvent
    }
}
