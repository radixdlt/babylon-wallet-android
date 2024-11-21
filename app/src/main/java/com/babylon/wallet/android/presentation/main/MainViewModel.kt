package com.babylon.wallet.android.presentation.main

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.AppLockStateProvider
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.deeplink.DeepLinkProcessingResult
import com.babylon.wallet.android.domain.usecases.deeplink.ProcessDeepLinkUseCase
import com.babylon.wallet.android.domain.usecases.p2plink.ObserveAccountsAndSyncWithConnectorExtensionUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.main.p2plinks.IncomingRequestsDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.hasNetworks
import rdx.works.core.sargon.isAdvancedLockEnabled
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.model.BackupServiceException.ClaimedByAnotherDevice
import rdx.works.profile.data.repository.CheckKeystoreIntegrityUseCase
import rdx.works.profile.domain.CheckEntitiesCreatedWithOlympiaUseCase
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val appEventBus: AppEventBus,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
    private val preferencesManager: PreferencesManager,
    private val checkKeystoreIntegrityUseCase: CheckKeystoreIntegrityUseCase,
    private val checkEntitiesCreatedWithOlympiaUseCase: CheckEntitiesCreatedWithOlympiaUseCase,
    private val observeAccountsAndSyncWithConnectorExtensionUseCase: ObserveAccountsAndSyncWithConnectorExtensionUseCase,
    private val cloudBackupErrorStream: CloudBackupErrorStream,
    private val processDeepLinkUseCase: ProcessDeepLinkUseCase,
    private val appLockStateProvider: AppLockStateProvider,
    private val incomingRequestsDelegate: IncomingRequestsDelegate
) : StateViewModel<MainViewModel.State>(), OneOffEventHandler<MainViewModel.Event> by OneOffEventHandlerImpl() {

    private var countdownJob: Job? = null

    val observeP2PLinks
        get() = incomingRequestsDelegate.observeP2PLinks

    val statusEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.Status>()

    val addressDetailsEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.AddressDetails>()

    val accountDeletedEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.AccountDeleted>()

    val accountsDetectedDeletedEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.AccountsPreviouslyDeletedDetected>()

    val accessFactorSourcesEvents = appEventBus
        .events
        .filterIsInstance<AppEvent.AccessFactorSources>()

    val isDevBannerVisible = getProfileUseCase.state.map { profileState ->
        when (profileState) {
            is ProfileState.Loaded -> {
                profileState.v1.currentGateway.network.id != NetworkId.MAINNET
            }

            else -> false
        }
    }

    val secureFolderWarning = appEventBus.events.filterIsInstance<AppEvent.SecureFolderWarning>()

    init {
        incomingRequestsDelegate(
            scope = viewModelScope,
            state = _state,
            oneOffEventHandler = this
        )

        observeProfileState()
        observeAppLockState()
        viewModelScope.launch { observeAccountsAndSyncWithConnectorExtensionUseCase() }
        processBufferedDeepLinkRequest()
    }

    override fun initialState(): State {
        return State(isDeviceSecure = deviceCapabilityHelper.isDeviceSecure)
    }

    private fun observeProfileState() {
        viewModelScope.launch {
            combine(
                getProfileUseCase.state,
                preferencesManager.isDeviceRootedDialogShown,
                cloudBackupErrorStream.errors
            ) { profileState, isDeviceRootedDialogShown, backupError ->
                val isAdvancedLockEnabled = if (profileState is ProfileState.Loaded) {
                    profileState.v1.isAdvancedLockEnabled
                } else {
                    false
                }

                _state.update {
                    State(
                        initialAppState = AppState.from(
                            profileState = profileState
                        ),
                        showDeviceRootedWarning = deviceCapabilityHelper.isDeviceRooted() && !isDeviceRootedDialogShown,
                        claimedByAnotherDeviceError = backupError as? ClaimedByAnotherDevice,
                        isAdvancedLockEnabled = isAdvancedLockEnabled,
                        isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
                    )
                }
            }.collect()
        }
    }

    private fun observeAppLockState() {
        viewModelScope.launch {
            appLockStateProvider.lockState
                .map { lockState ->
                    lockState == AppLockStateProvider.LockState.Locked
                }
                .distinctUntilChanged()
                .collect { isAppLocked ->
                    _state.update { state ->
                        state.copy(isAppLocked = isAppLocked)
                    }
                }
        }
    }

    private fun processBufferedDeepLinkRequest() {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.ProcessBufferedDeepLinkRequest>().collect {
                incomingRequestRepository.consumeBufferedRequest()?.let { request ->
                    incomingRequestsDelegate.verifyIncomingRequest(request)
                }
            }
        }
    }

    fun onHighPriorityScreen() = viewModelScope.launch {
        incomingRequestRepository.pauseIncomingRequests()
    }

    fun onLowPriorityScreen() = viewModelScope.launch {
        incomingRequestRepository.resumeIncomingRequests()
    }

    fun handleDeepLink(deepLink: Uri) {
        viewModelScope.launch {
            processDeepLinkUseCase(deepLink.toString()).onSuccess { result ->
                when (result) {
                    is DeepLinkProcessingResult.Processed -> {
                        incomingRequestsDelegate.verifyIncomingRequest(result.request)
                    }

                    DeepLinkProcessingResult.Buffered -> {
                        _state.update {
                            it.copy(showMobileConnectWarning = true)
                        }
                    }
                }
            }.onFailure { error ->
                Timber.d(error)
            }
        }
    }

    fun onMobileConnectWarningShown() {
        _state.update {
            it.copy(showMobileConnectWarning = false)
        }
    }

    fun onInvalidRequestMessageShown() {
        _state.update { it.copy(dappRequestFailure = null) }
    }

    fun clearOlympiaError() {
        _state.update { it.copy(olympiaErrorState = null) }
    }

    fun onAppToForeground() {
        val isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
        _state.update { state ->
            state.copy(isDeviceSecure = isDeviceSecure)
        }
        if (!_state.value.isAppLocked) {
            runForegroundChecks()
        }
    }

    private fun runForegroundChecks() {
        viewModelScope.launch {
            checkKeystoreIntegrityUseCase()
            if (_state.value.isDeviceSecure) {
                val checkResult = checkEntitiesCreatedWithOlympiaUseCase()
                if (checkResult.isAnyEntityCreatedWithOlympia) {
                    _state.update { state ->
                        state.copy(
                            olympiaErrorState = OlympiaErrorState(
                                affectedAccounts = checkResult.affectedAccounts,
                                affectedPersonas = checkResult.affectedPersonas
                            )
                        )
                    }
                    countdownJob?.cancel()
                    countdownJob = startOlympiaErrorCountdown()
                    return@launch
                }
            }
        }
    }

    private fun startOlympiaErrorCountdown(): Job {
        return viewModelScope.launch {
            while (isActive && state.value.olympiaErrorState?.isCountdownActive == true) {
                delay(TICK_MS)
                _state.update { state ->
                    state.copy(
                        olympiaErrorState = state.olympiaErrorState?.copy(
                            secondsLeft = state.olympiaErrorState.secondsLeft - 1
                        )
                    )
                }
            }
        }
    }

    data class State(
        val initialAppState: AppState = AppState.Loading,
        val showDeviceRootedWarning: Boolean = false,
        val dappRequestFailure: RadixWalletException.DappRequestException? = null,
        val olympiaErrorState: OlympiaErrorState? = null,
        val claimedByAnotherDeviceError: ClaimedByAnotherDevice? = null,
        val showMobileConnectWarning: Boolean = false,
        val isAdvancedLockEnabled: Boolean = false,
        val isAppLocked: Boolean = false,
        val isDeviceSecure: Boolean
    ) : UiState {
        val showDeviceNotSecureDialog: Boolean
            get() = !isDeviceSecure && !isAdvancedLockEnabled
    }

    data class OlympiaErrorState(
        val secondsLeft: Int = 30,
        val affectedAccounts: List<Account>,
        val affectedPersonas: List<Persona>
    ) {
        val isCountdownActive: Boolean
            get() = secondsLeft > 0
    }

    sealed class Event : OneOffEvent {
        data class IncomingRequestEvent(val request: DappToWalletInteraction) : Event()
    }

    companion object {
        private const val TICK_MS = 1000L
    }
}

sealed interface AppState {
    data object OnBoarding : AppState
    data object Wallet : AppState
    data class IncompatibleProfile(val cause: CommonException) : AppState
    data object Loading : AppState

    companion object {
        fun from(
            profileState: ProfileState
        ) = when (profileState) {
            is ProfileState.Incompatible -> IncompatibleProfile(cause = profileState.v1)
            is ProfileState.Loaded -> if (profileState.v1.hasNetworks) {
                Wallet
            } else {
                OnBoarding
            }

            is ProfileState.None -> OnBoarding
        }
    }
}
