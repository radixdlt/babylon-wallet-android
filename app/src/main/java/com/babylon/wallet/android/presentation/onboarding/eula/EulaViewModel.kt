package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.GoogleSignInManager
import javax.inject.Inject

@HiltViewModel
class EulaViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : ViewModel(), OneOffEventHandler<EulaViewModel.EulaEvent> by OneOffEventHandlerImpl() {

    fun onAcceptClick() = viewModelScope.launch {
        sendEvent(EulaEvent.ProceedToCreateNewWallet(isWithCloudBackupEnabled = googleSignInManager.isCloudBackupAuthorized()))
    }

    fun onBackClick() = viewModelScope.launch {
        sendEvent(EulaEvent.NavigateBack(isWithCloudBackupEnabled = googleSignInManager.isCloudBackupAuthorized()))
    }

    sealed interface EulaEvent : OneOffEvent {
        data class ProceedToCreateNewWallet(val isWithCloudBackupEnabled: Boolean) : EulaEvent
        data class NavigateBack(val isWithCloudBackupEnabled: Boolean) : EulaEvent
    }
}
