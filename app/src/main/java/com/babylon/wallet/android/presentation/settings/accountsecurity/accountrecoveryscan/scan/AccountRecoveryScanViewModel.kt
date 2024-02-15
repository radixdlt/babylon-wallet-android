package com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.AddRecoveredAccountsToProfileUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceByIdValue
import javax.inject.Inject

@HiltViewModel
class AccountRecoveryScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val addRecoveredAccountsToProfileUseCase: AddRecoveredAccountsToProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<AccountRecoveryScanViewModel.State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = AccountRecoveryScanArgs(savedStateHandle)

    // used only when account recovery scan is from onboarding
    private var givenTempMnemonic: MnemonicWithPassphrase? = null
    private var nextDerivationPathOffset: Int = 0

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSource = args.factorSourceId?.let { factorSourceId ->
                getProfileUseCase.factorSourceByIdValue(factorSourceId) as FactorSource.CreatingEntity
            }
            _state.update { state ->
                state.copy(
                    recoveryFactorSource = factorSource,
                    isOlympiaSeedPhrase = args.isOlympia == true
                )
            }

            // if true it is account scan from recovery in onboarding with a given main babylon seed phrase
            if (factorSource == null && args.mnemonic != null && args.passphrase != null) {
                givenTempMnemonic = MnemonicWithPassphrase(
                    mnemonic = args.mnemonic,
                    bip39Passphrase = args.passphrase
                )
                givenTempMnemonic?.let {
                    val mainBabylonDeviceFactorSource = ensureBabylonFactorSourceExistUseCase.initMainBabylonFactorSourceWithMnemonic(
                        mnemonic = it
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
                isMainBabylonFactorSource = args.factorSourceId == null && args.mnemonic != null && args.passphrase != null,
                factorSource = recoveryFactorSource,
                isOlympia = args.isOlympia == true
            )
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun startRecoveryScan(
        isMainBabylonFactorSource: Boolean,
        factorSource: FactorSource.CreatingEntity,
        isOlympia: Boolean
    ) {
        viewModelScope.launch {
            val output = if (isMainBabylonFactorSource) { // account scan from onboarding with a given main babylon seed phrase
                accessFactorSourcesProxy.reDeriveAccounts(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToReDerivePublicKey.WithGivenMnemonic(
                        mnemonicWithPassphrase = givenTempMnemonic!!,
                        factorSource = factorSource,
                        nextDerivationPathOffset = nextDerivationPathOffset
                    )
                )
            } else {
                accessFactorSourcesProxy.reDeriveAccounts(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToReDerivePublicKey.WithGivenFactorSource(
                        factorSource = factorSource,
                        isForLegacyOlympia = isOlympia,
                        nextDerivationPathOffset = nextDerivationPathOffset
                    )
                )
            }
            output.onSuccess { recoveredAccountsWithOnLedgerStatus ->
                if (isActive) {
                    nextDerivationPathOffset = recoveredAccountsWithOnLedgerStatus.nextDerivationPathOffset
                    deriveStateFromRecoveredAccounts(recoveredAccountsWithOnLedgerStatus.data)
                }
            }.onFailure {
                sendEvent(Event.CloseScan)
            }
        }
    }

    private fun deriveStateFromRecoveredAccounts(accounts: List<AccountWithOnLedgerStatus>) {
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
                inactiveAccounts = inactiveAccounts.map { Selectable(it) }.toPersistentList()
            )
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
                    _state.update { it.copy(isRestoring = false) }
                    return@launch
                }
                _state.update { it.copy(isRestoring = true) }
                val bdfs = state.value.recoveryFactorSource
                val accounts = state.value.activeAccounts +
                    state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                generateProfileUseCase.initWithBdfsAndAccounts(
                    bdfs = bdfs as DeviceFactorSource,
                    mnemonicWithPassphrase = givenTempMnemonic!!,
                    accounts = accounts.toIdentifiedArrayList()
                )
                sendEvent(Event.RecoverComplete)
            } else {
                val accounts = state.value.activeAccounts +
                    state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                _state.update { it.copy(isRestoring = true) }
                if (accounts.isNotEmpty()) {
                    val authenticated = biometricAuthenticationProvider()
                    if (authenticated.not()) {
                        _state.update { it.copy(isRestoring = false) }
                        return@launch
                    }
                    addRecoveredAccountsToProfileUseCase(accounts = accounts)
                    sendEvent(Event.RecoverComplete)
                }
                sendEvent(Event.RecoverComplete)
            }
        }
    }

    fun onAccountSelected(selectableAccount: Selectable<Network.Account>) {
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
        val recoveryFactorSource: FactorSource.CreatingEntity? = null,
        val isOlympiaSeedPhrase: Boolean = false,
        val contentState: ContentState = ContentState.ScanInProgress,
        val recoveredAccounts: ImmutableList<AccountWithOnLedgerStatus> = persistentListOf(),
        val activeAccounts: PersistentList<Network.Account> = persistentListOf(),
        val inactiveAccounts: PersistentList<Selectable<Network.Account>> = persistentListOf(),
        val isRestoring: Boolean = false,
        val isNoMnemonicErrorVisible: Boolean = false
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
