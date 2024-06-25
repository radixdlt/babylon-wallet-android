package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.Accounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.TimestampGenerator
import rdx.works.core.mapWhen
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.domain.AddRecoveredAccountsToProfileUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountRecoveryScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val addRecoveredAccountsToProfileUseCase: AddRecoveredAccountsToProfileUseCase,
    private val appEventBus: AppEventBus,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository,
    private val deviceInfoRepository: DeviceInfoRepository
) : StateViewModel<AccountRecoveryScanViewModel.State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = AccountRecoveryScanArgs(savedStateHandle)

    // used only when account recovery scan is from onboarding
    private var givenTempMnemonic: MnemonicWithPassphrase? = null
    private var nextDerivationPathOffset: UInt = 0u

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSource = args.factorSourceId?.let { factorSourceId ->
                getProfileUseCase().factorSourceById(factorSourceId)
            }
            _state.update { state ->
                state.copy(
                    recoveryFactorSource = factorSource,
                    isOlympiaSeedPhrase = args.isOlympia == true
                )
            }

            // if true it is account scan from recovery in onboarding with a given main babylon seed phrase
            if (factorSource == null) {
                givenTempMnemonic = accessFactorSourcesProxy.getTempMnemonicWithPassphrase()
                givenTempMnemonic?.let { mnemonic ->
                    val deviceInfo = deviceInfoRepository.getDeviceInfo()
                    val mainBabylonDeviceFactorSource = FactorSource.Device.babylon(
                        mnemonicWithPassphrase = mnemonic,
                        model = deviceInfo.model,
                        name = deviceInfo.name,
                        createdAt = TimestampGenerator(),
                        isMain = true
                    )
                    _state.update { state ->
                        state.copy(recoveryFactorSource = mainBabylonDeviceFactorSource)
                    }
                    startRecoveryScan(
                        isMainBabylonFactorSource = true,
                        factorSource = mainBabylonDeviceFactorSource,
                        isOlympia = false
                    )
                }
            } else { // else it is account scan from account settings
                val recoveryFactorSource = state.value.recoveryFactorSource
                if (recoveryFactorSource != null) { // this is always true
                    startRecoveryScan(
                        isMainBabylonFactorSource = false,
                        factorSource = recoveryFactorSource,
                        isOlympia = args.isOlympia == true
                    )
                }
            }
        }
    }

    fun onScanMoreClick() {
        val recoveryFactorSource = state.value.recoveryFactorSource
        if (recoveryFactorSource != null) { // this is always true
            startRecoveryScan(
                isMainBabylonFactorSource = args.factorSourceId == null && givenTempMnemonic != null,
                factorSource = recoveryFactorSource,
                isOlympia = args.isOlympia == true
            )
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun startRecoveryScan(
        isMainBabylonFactorSource: Boolean,
        factorSource: FactorSource,
        isOlympia: Boolean
    ) {
        viewModelScope.launch {
            val output = if (isMainBabylonFactorSource) { // account scan from onboarding with a given main babylon seed phrase
                accessFactorSourcesProxy.reDeriveAccounts(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToReDeriveAccounts.WithGivenMnemonic(
                        mnemonicWithPassphrase = givenTempMnemonic!!,
                        factorSource = factorSource,
                        nextDerivationPathOffset = nextDerivationPathOffset
                    )
                )
            } else {
                accessFactorSourcesProxy.reDeriveAccounts(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToReDeriveAccounts.WithGivenFactorSource(
                        factorSource = factorSource,
                        isForLegacyOlympia = isOlympia,
                        nextDerivationPathOffset = nextDerivationPathOffset
                    )
                )
            }
            output.onSuccess { derivedAccountsWithNextDerivationPath ->
                if (isActive) {
                    nextDerivationPathOffset = derivedAccountsWithNextDerivationPath.nextDerivationPathOffset
                    resolveStateFromDerivedAccounts(derivedAccountsWithNextDerivationPath.derivedAccounts)
                }
            }.onFailure { e ->
                if (e is ProfileException.NoMnemonic) {
                    _state.update { it.copy(isNoMnemonicErrorVisible = true) }
                } else {
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(e)) }
                    delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                    sendEvent(Event.CloseScan)
                }
            }
        }
    }

    private suspend fun resolveStateFromDerivedAccounts(derivedAccounts: List<Account>) {
        _state.update { it.copy(isScanningNetwork = true) }

        val accountsWithLedgerState = resolveAccountsLedgerStateRepository(derivedAccounts)

        accountsWithLedgerState.onSuccess { accounts ->
            val allRecoveredAccounts = state.value.recoveredAccounts + accounts
            val activeAccounts = allRecoveredAccounts
                .filter { it.status == AccountWithOnLedgerStatus.Status.Active }
                .map { it.account }.toPersistentList()

            val maxActiveIndex = allRecoveredAccounts.indexOfLast { it.status == AccountWithOnLedgerStatus.Status.Active }
            val inactiveAccounts = if (maxActiveIndex == -1) {
                persistentListOf()
            } else {
                allRecoveredAccounts.subList(0, maxActiveIndex)
                    .filter { it.status == AccountWithOnLedgerStatus.Status.Inactive }
                    .map { it.account }
                    .toPersistentList()
            }

            _state.update { state ->
                state.copy(
                    contentState = State.ContentState.ScanComplete,
                    recoveredAccounts = allRecoveredAccounts.toPersistentList(),
                    activeAccounts = activeAccounts,
                    inactiveAccounts = inactiveAccounts.map { Selectable(it) }.toPersistentList(),
                    isScanningNetwork = false
                )
            }
        }.onFailure {
            _state.update { state -> state.copy(isScanningNetwork = false) }
            sendEvent(Event.CloseScan)
        }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
        viewModelScope.launch {
            sendEvent(Event.CloseScan)
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    fun onContinueClick(biometricAuthenticationProvider: suspend () -> Boolean) {
        viewModelScope.launch {
            if (givenTempMnemonic != null) { // account scan from onboarding with a given main babylon seed phrase
                val authenticated = biometricAuthenticationProvider()
                if (authenticated.not()) {
                    _state.update { it.copy(isScanningNetwork = false) }
                    return@launch
                }
                _state.update { it.copy(isScanningNetwork = true) }
                val bdfs = state.value.recoveryFactorSource
                val accounts = state.value.activeAccounts +
                    state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                generateProfileUseCase.derived(
                    deviceFactorSource = bdfs as FactorSource.Device,
                    mnemonicWithPassphrase = givenTempMnemonic!!,
                    accounts = Accounts(accounts)
                ).onSuccess {
                    sendEvent(Event.RecoverComplete)
                }.onFailure { error ->
                    if (error is ProfileException.SecureStorageAccess) {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    } else {
                        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                    }
                    _state.update { it.copy(isScanningNetwork = false) }
                }
            } else {
                _state.update { it.copy(isScanningNetwork = true) }
                val accounts = state.value.activeAccounts +
                    state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                if (accounts.isNotEmpty()) {
                    addRecoveredAccountsToProfileUseCase(accounts = accounts)
                }
                sendEvent(Event.RecoverComplete)
            }
        }
    }

    fun onAccountSelected(selectableAccount: Selectable<Account>) {
        _state.update { state ->
            state.copy(
                inactiveAccounts = state.inactiveAccounts.mapWhen(
                    predicate = { it.data.address == selectableAccount.data.address },
                    mutation = {
                        it.copy(selected = !it.selected)
                    }
                ).toPersistentList(),
            )
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onBackClick() {
        viewModelScope.launch {
            sendEvent(Event.OnBackClick)
            _state.update { State() }
        }
    }

    companion object {
        const val ACCOUNTS_PER_SCAN = 50
    }

    data class State(
        val recoveryFactorSource: FactorSource? = null,
        val isOlympiaSeedPhrase: Boolean = false,
        val contentState: ContentState = ContentState.ScanInProgress,
        val recoveredAccounts: ImmutableList<AccountWithOnLedgerStatus> = persistentListOf(),
        val activeAccounts: PersistentList<Account> = persistentListOf(),
        val inactiveAccounts: PersistentList<Selectable<Account>> = persistentListOf(),
        val isScanningNetwork: Boolean = false,
        val isNoMnemonicErrorVisible: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState {

        enum class ContentState {
            ScanInProgress,
            ScanComplete
        }
    }
}

sealed interface Event : OneOffEvent {
    data object CloseScan : Event
    data object RecoverComplete : Event
    data object OnBackClick : Event
}
