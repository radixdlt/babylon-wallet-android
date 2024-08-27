package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.AppLockStateProvider
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.RadixWalletException.CloudBackupException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.CanSignInToGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.cloudbackup.model.BackupServiceException
import rdx.works.profile.cloudbackup.model.GoogleAccount
import rdx.works.profile.domain.backup.ChangeBackupSettingUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConnectCloudBackupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val googleSignInManager: GoogleSignInManager,
    private val checkMigrationToNewBackupSystemUseCase: CheckMigrationToNewBackupSystemUseCase,
    private val changeBackupSettingUseCase: ChangeBackupSettingUseCase,
    private val appLockStateProvider: AppLockStateProvider,
    @ApplicationScope private val appScope: CoroutineScope
) : StateViewModel<ConnectCloudBackupViewModel.State>(),
    CanSignInToGoogle,
    OneOffEventHandler<ConnectCloudBackupViewModel.Event> by OneOffEventHandlerImpl() {

    private val args: ConnectCloudBackupArgs = ConnectCloudBackupArgs(savedStateHandle)

    override fun initialState(): State = State(mode = args.mode)

    override fun signInManager(): GoogleSignInManager = googleSignInManager

    override fun onSignInResult(result: Result<GoogleAccount>) {
        viewModelScope.launch {
            result.onSuccess { googleAccount ->
                Timber.tag("CloudBackup").d("\uD83D\uDD11 Authorized for email: ${googleAccount.email}")
                _state.update { it.copy(isConnecting = false) }

                if (state.value.mode == ConnectMode.ExistingWallet) {
                    // For existing users only who migrate to the new backup system, request an eager backup as soon as they opt in
                    // We do that by "updating" the profile, thus updating the newly introduced device id.
                    // See also in ProfileRepositoryImpl.saveProfile() the related comment.
                    changeBackupSettingUseCase(isChecked = true)
                }

                sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = true))
            }.onFailure { exception ->
                _state.update { state -> state.copy(isConnecting = false) }

                if (exception is BackupServiceException.UnauthorizedException) {
                    _state.update { it.copy(errorMessage = UiMessage.ErrorMessage(CloudBackupException(exception))) }
                } else {
                    Timber.tag("CloudBackup").w(exception)
                }
            }
            appLockStateProvider.resumeLocking()
        }
    }

    fun onLoginToGoogleClick() = viewModelScope.launch {
        _state.update { it.copy(isConnecting = true) }

        if (googleSignInManager.isSignedIn()) {
            googleSignInManager.signOut()
        }

        checkIfExistingWalletAndRevokeAccessToDeprecatedCloud()
        appLockStateProvider.pauseLocking()
        sendEvent(Event.SignInToGoogle)
    }

    fun onBackPress() {
        appScope.launch {
            googleSignInManager.signOut()
            sendEvent(Event.Close)
        }
    }

    fun onErrorMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onSkipClick() = viewModelScope.launch {
        if (googleSignInManager.isSignedIn()) {
            googleSignInManager.signOut()
        }

        checkIfExistingWalletAndRevokeAccessToDeprecatedCloud()

        sendEvent(Event.Proceed(mode = state.value.mode, isCloudBackupEnabled = false))
    }

    private suspend fun checkIfExistingWalletAndRevokeAccessToDeprecatedCloud() {
        if (state.value.mode == ConnectMode.ExistingWallet) {
            checkMigrationToNewBackupSystemUseCase.revokeAccessToDeprecatedCloudBackup()
        }
    }

    data class State(
        val mode: ConnectMode,
        val isConnecting: Boolean = false,
        val errorMessage: UiMessage? = null
    ) : UiState

    enum class ConnectMode {
        NewWallet,
        RestoreWallet,
        ExistingWallet
    }

    sealed interface Event : OneOffEvent {
        data object Close : Event
        data object SignInToGoogle : Event
        data class Proceed(val mode: ConnectMode, val isCloudBackupEnabled: Boolean) : Event
    }
}
