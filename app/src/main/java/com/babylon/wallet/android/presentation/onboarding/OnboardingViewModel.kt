package com.babylon.wallet.android.presentation.onboarding

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.googlesignin.GoogleSignInManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : StateViewModel<OnboardingViewModel.OnBoardingUiState>(),
    OneOffEventHandler<OnboardingViewModel.OnBoardingEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): OnBoardingUiState = OnBoardingUiState()

    fun turnOnCloudBackup() = viewModelScope.launch {
        val intent = googleSignInManager.createSignInIntent()
        sendEvent(OnBoardingEvent.SignInToGoogle(intent))
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            googleSignInManager.handleSignInResult(result)
                .onSuccess { googleAccount ->
                    _state.update { it.copy(backupEmail = googleAccount.email ?: "!") }
                    Timber.d("cloud backup is authorized")
                }
                .onFailure { exception ->
                    _state.update { state -> state.copy(backupEmail = exception.message.orEmpty()) }
                    Timber.e("cloud backup authorization failed: ${exception.message}")
                }
        }
    }

    fun onCreateNewWalletClick() {
        viewModelScope.launch {
            sendEvent(OnBoardingEvent.CreateNewWallet)
        }
    }

    data class OnBoardingUiState(
        val backupEmail: String = ""
    ) : UiState

    sealed interface OnBoardingEvent : OneOffEvent {
        data object CreateNewWallet : OnBoardingEvent
        data class SignInToGoogle(val signInIntent: Intent) : OnBoardingEvent
    }
}
