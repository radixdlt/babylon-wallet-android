package com.babylon.wallet.android.presentation.main

import android.net.Uri
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.AppLockStateProvider
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.discover.DiscoverRepository
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.usecases.deeplink.DeepLinkProcessingResult
import com.babylon.wallet.android.domain.usecases.deeplink.ProcessDeepLinkUseCase
import com.babylon.wallet.android.domain.usecases.p2plink.ObserveAccountsAndSyncWithConnectorExtensionUseCase
import com.babylon.wallet.android.presentation.alerts.AlertHandler
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.main.p2plinks.IncomingRequestsDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.SargonOsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.hasNetworks
import rdx.works.core.sargon.isAdvancedLockEnabled
import rdx.works.core.sargon.isCurrentNetworkMainnet
import rdx.works.profile.cloudbackup.domain.CloudBackupErrorStream
import rdx.works.profile.cloudbackup.model.BackupServiceException.ClaimedByAnotherDevice
import rdx.works.profile.data.repository.CheckKeystoreIntegrityUseCase
import rdx.works.profile.domain.FirstAccountCreationStatus
import rdx.works.profile.domain.FirstAccountCreationStatusManager
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val createInitialAccountStatusSemaphore: FirstAccountCreationStatusManager,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val appEventBus: AppEventBus,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
    private val preferencesManager: PreferencesManager,
    private val checkKeystoreIntegrityUseCase: CheckKeystoreIntegrityUseCase,
    private val observeAccountsAndSyncWithConnectorExtensionUseCase: ObserveAccountsAndSyncWithConnectorExtensionUseCase,
    private val cloudBackupErrorStream: CloudBackupErrorStream,
    private val processDeepLinkUseCase: ProcessDeepLinkUseCase,
    private val appLockStateProvider: AppLockStateProvider,
    private val incomingRequestsDelegate: IncomingRequestsDelegate,
    private val sargonOsManager: SargonOsManager,
    private val discoverRepository: DiscoverRepository,
    private val alertHandler: AlertHandler
) : StateViewModel<MainViewModel.State>(),
    OneOffEventHandler<MainViewModel.Event> by OneOffEventHandlerImpl() {

    val statusEvents = appEventBus.events
        .filterIsInstance<AppEvent.Status>()

    val addressDetailsEvents = appEventBus.events
        .filterIsInstance<AppEvent.AddressDetails>()

    val accountDeletedEvents = appEventBus.events
        .filterIsInstance<AppEvent.AccountDeleted>()

    val accountsDetectedDeletedEvents = appEventBus.events
        .filterIsInstance<AppEvent.AccountsPreviouslyDeletedDetected>()

    val accessFactorSourcesEvents = appEventBus.events
        .filterIsInstance<AppEvent.AccessFactorSources>()

    val addFactorSourceEvents = appEventBus.events
        .filterIsInstance<AppEvent.AddFactorSource>()

    val selectFactorSourceEvents = appEventBus.events
        .filterIsInstance<AppEvent.SelectFactorSource>()

    val fixSecurityIssueEvents = appEventBus.events
        .filterIsInstance<AppEvent.FixSecurityIssue>()

    val nfcEvents = appEventBus.events
        .filterIsInstance<AppEvent.Nfc>()

    val isDevBannerVisible = getProfileUseCase.state.map { profileState ->
        when (profileState) {
            is ProfileState.Loaded -> {
                profileState.v1.currentGateway.network.id != NetworkId.MAINNET
            }

            else -> false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )

    val secureFolderWarning = appEventBus.events.filterIsInstance<AppEvent.SecureFolderWarning>()

    val themeSelection = preferencesManager
        .themeSelection
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    init {
        incomingRequestsDelegate(
            scope = viewModelScope,
            state = _state,
            oneOffEventHandler = this
        )

        observeAppState()
        observeAppLockState()
        viewModelScope.launch { observeAccountsAndSyncWithConnectorExtensionUseCase() }
        processBufferedDeepLinkRequest()
    }

    override fun initialState(): State {
        return State(isDeviceSecure = deviceCapabilityHelper.isDeviceSecure)
    }

    private fun observeAppState() {
        combine(
            sargonOsManager.sargonState,
            getProfileUseCase.state.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = null
            ),
            createInitialAccountStatusSemaphore.firstAccountCreationStatus,
            preferencesManager.isDeviceRootedDialogShown,
            cloudBackupErrorStream.errors
        ) { osState, profileState, firstAccountCreationStatus, isDeviceRootedDialogShown, backupError ->
            val isAdvancedLockEnabled = if (profileState is ProfileState.Loaded) {
                profileState.v1.isAdvancedLockEnabled
            } else {
                false
            }

            val tabs = if (profileState is ProfileState.Loaded) {
                if (profileState.v1.isCurrentNetworkMainnet) {
                    MainTab.mainnet
                } else {
                    MainTab.testnet
                }
            } else {
                MainTab.mainnet
            }

            _state.update {
                it.copy(
                    initialAppState = AppState.from(
                        sargonOsState = osState,
                        profileState = profileState,
                        firstAccountCreationStatus = firstAccountCreationStatus
                    ),
                    showDeviceRootedWarning = deviceCapabilityHelper.isDeviceRooted() && !isDeviceRootedDialogShown,
                    claimedByAnotherDeviceError = backupError as? ClaimedByAnotherDevice,
                    isAdvancedLockEnabled = isAdvancedLockEnabled,
                    tabs = tabs,
                    selectedTab = if (profileState !is ProfileState.Loaded) MainTab.Wallet else it.selectedTab,
                    isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
                )
            }
        }.launchIn(viewModelScope)
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

    fun onAppToForeground() {
        val isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
        _state.update { state ->
            state.copy(isDeviceSecure = isDeviceSecure)
        }
        if (!_state.value.isAppLocked) {
            runForegroundChecks()
        }

//        checkNewBlogPost() TODO re-enable when needed
    }

    fun onBeforeBiometricsRequest() {
        // Avoids double biometrics request. In older Android versions < 30 pin/pattern unlock
        // used to be invoked in a different activity. This would result in biometrics being
        // requested once by the WalletInteractor and then another time by advanced lock
        // (if enabled), since the app would return to the foreground.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            appLockStateProvider.pauseLocking()
        }
    }

    fun onAfterBiometricsResult() {
        // Resumes app lock feature when biometrics respond to WalletInteractor
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            appLockStateProvider.resumeLocking()
        }
    }

    fun onTabClick(tab: MainTab) {
        if (tab != state.value.selectedTab) {
            _state.update { it.copy(selectedTab = tab) }
        }
    }

    private fun runForegroundChecks() {
        viewModelScope.launch {
            checkKeystoreIntegrityUseCase()
        }
    }

    @Suppress("unused")
    private fun checkNewBlogPost() {
        viewModelScope.launch {
            val newBlogPost = discoverRepository.getNewBlogPost().getOrNull() ?: return@launch

            alertHandler.show(
                AlertHandler.State.NewBlogPost(
                    post = newBlogPost
                )
            )
        }
    }

    data class State(
        val initialAppState: AppState = AppState.Loading,
        val showDeviceRootedWarning: Boolean = false,
        val dappRequestFailure: UiMessage.ErrorMessage? = null,
        val claimedByAnotherDeviceError: ClaimedByAnotherDevice? = null,
        val showMobileConnectWarning: Boolean = false,
        val isAdvancedLockEnabled: Boolean = false,
        val isAppLocked: Boolean = false,
        val tabs: Set<MainTab> = MainTab.mainnet,
        val selectedTab: MainTab = MainTab.Wallet,
        val isDeviceSecure: Boolean
    ) : UiState {
        val showDeviceNotSecureDialog: Boolean
            get() = !isDeviceSecure && !isAdvancedLockEnabled
    }

    sealed class Event : OneOffEvent {
        data class IncomingRequestEvent(val request: DappToWalletInteraction) : Event()
    }
}

sealed interface AppState {
    data object OnBoarding : AppState
    data object Wallet : AppState
    data class IncompatibleProfile(val cause: CommonException) : AppState
    data class ErrorBootingSargon(val error: Throwable) : AppState
    data object Loading : AppState

    companion object {
        fun from(
            sargonOsState: SargonOsState,
            profileState: ProfileState?,
            firstAccountCreationStatus: FirstAccountCreationStatus
        ) = when (sargonOsState) {
            is SargonOsState.Idle -> Loading
            is SargonOsState.BootError -> ErrorBootingSargon(sargonOsState.error)
            is SargonOsState.Booted -> {
                when (profileState) {
                    is ProfileState.Incompatible -> IncompatibleProfile(cause = profileState.v1)
                    is ProfileState.Loaded -> if (
                        profileState.v1.hasNetworks &&
                        firstAccountCreationStatus == FirstAccountCreationStatus.None
                    ) {
                        Wallet
                    } else {
                        OnBoarding
                    }

                    is ProfileState.None -> OnBoarding
                    null -> Loading
                }
            }
        }
    }
}

enum class MainTab(val route: String) {
    Wallet("tab_wallet"),
    DApps("tab_dapps"),
    Discover("tab_discover"),
    Settings("tab_settings");

    companion object {
        val mainnet: Set<MainTab> = setOf(
            Wallet,
            DApps,
            Discover,
            Settings
        )

        val testnet: Set<MainTab> = setOf(
            Wallet,
            DApps,
            Discover,
            Settings
        )
    }
}
