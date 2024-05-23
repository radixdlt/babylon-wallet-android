package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import javax.inject.Inject

@HiltViewModel
class EulaViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : ViewModel(), OneOffEventHandler<EulaViewModel.EulaEvent> by OneOffEventHandlerImpl() {

    init {
        // if user login but then decides to leave the [CreateAccountScreen] and navigates to the [OnboardingScreen]
        // or kills the app before is in [CreateAccountScreen],
        // then the next time the wallet navigates to [EulaScreen] ensure to revoke access.
        //
        // If we don't do this, and e.g. user kills the app, at the next launch they won't be able to login!
        //
        // This won't change the fact that when user navigates from [CreateAccountScreen] to [EulaScreen] and
        // previously has already logged in to Google Drive, they WON'T see connect to cloud screen. That's a requirement.
        revokeAccessToGoogleDrive()
    }

    private fun revokeAccessToGoogleDrive() {
        viewModelScope.launch {
            googleSignInManager.signOut()
        }
    }

    fun onAcceptClick() = viewModelScope.launch {
        sendEvent(EulaEvent.ProceedToCreateNewWallet(isWithCloudBackupEnabled = googleSignInManager.isSignedIn()))
    }

    fun onBackClick() = viewModelScope.launch {
        sendEvent(EulaEvent.NavigateBack(isWithCloudBackupEnabled = googleSignInManager.isSignedIn()))
    }

    sealed interface EulaEvent : OneOffEvent {
        data class ProceedToCreateNewWallet(val isWithCloudBackupEnabled: Boolean) : EulaEvent
        data class NavigateBack(val isWithCloudBackupEnabled: Boolean) : EulaEvent
    }
}
