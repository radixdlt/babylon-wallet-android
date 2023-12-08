package com.babylon.wallet.android.presentation.account.recover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.RecoverAccountsForFactorSourceUseCase
import com.babylon.wallet.android.presentation.account.recover.scan.AccountRecoveryScanArgs
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.AddRecoveredAccountsToProfileUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceByIdValue
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountRecoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recoverAccountsForFactorSourceUseCase: RecoverAccountsForFactorSourceUseCase,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val generateProfileUseCase: GenerateProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val addRecoveredAccountsToProfileUseCase: AddRecoveredAccountsToProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<AccountRecoveryViewModel.State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = AccountRecoveryScanArgs(savedStateHandle)

    private var runningScan: Job? = null

    override fun initialState(): State {
        return State()
    }

    init {
        viewModelScope.launch {
            recoverAccountsForFactorSourceUseCase.interactionState.collect { interactionState ->
                _state.update { state ->
                    state.copy(interactionState = interactionState)
                }
            }
        }
    }

    fun startScanForExistingFactorSource() {
        viewModelScope.launch {
            args.factorSourceId?.let { factorSourceIdValue ->
                val factorSource = getProfileUseCase.factorSourceByIdValue(factorSourceIdValue) ?: return@launch
                when (factorSource) {
                    is DeviceFactorSource -> {
                        val mnemonic = mnemonicRepository.readMnemonic(factorSource.id).getOrThrow()
                        _state.update {
                            it.copy(
                                recoveryFactorSource = RecoveryFactorSource.Device(factorSource, mnemonic, args.isOlympia == true)
                            )
                        }
                        startRecoveryScan()
                    }

                    is LedgerHardwareWalletFactorSource -> {
                        _state.update {
                            it.copy(
                                recoveryFactorSource = RecoveryFactorSource.Ledger(
                                    factorSource,
                                    args.isOlympia == true
                                )
                            )
                        }
                        startRecoveryScan()
                    }

                    else -> error("Unsupported factor source type")
                }
            }
        }
    }

    fun initDeviceFactorSource(mnemonicWithPassphrase: MnemonicWithPassphrase, isMain: Boolean = false) {
        recoverAccountsForFactorSourceUseCase.reset()
        val factorSource =
            ensureBabylonFactorSourceExistUseCase.initBabylonFactorSourceWithMnemonic(mnemonicWithPassphrase, isMain = isMain)
        _state.update {
            it.copy(
                recoveryFactorSource = RecoveryFactorSource.VirtualDeviceFactorSource(
                    factorSource,
                    mnemonicWithPassphrase
                )
            )
        }
    }

    fun onDismissSigningStatusDialog() {
        runningScan?.cancel()
        _state.update { it.copy(interactionState = null) }
        viewModelScope.launch {
            sendEvent(Event.CloseScan)
        }
    }

    fun onContinueClick() {
        when (val recoveryFS = state.value.recoveryFactorSource) {
            is RecoveryFactorSource.Ledger,
            is RecoveryFactorSource.Device -> {
                val accounts = state.value.activeAccounts + state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                viewModelScope.launch {
                    _state.update { it.copy(isRestoring = true) }
                    addRecoveredAccountsToProfileUseCase(accounts = accounts)
                    sendEvent(Event.RecoverComplete)
                }
            }

            is RecoveryFactorSource.VirtualDeviceFactorSource -> {
                val bdfs = recoveryFS.virtualDeviceFactorSource
                val mnemonicWithPassphrase = recoveryFS.mnemonicWithPassphrase
                val accounts = state.value.activeAccounts + state.value.inactiveAccounts.filter { it.selected }.map { it.data }
                viewModelScope.launch {
                    _state.update { it.copy(isRestoring = true) }
                    generateProfileUseCase.initWithBdfsAndAccounts(
                        bdfs = bdfs,
                        mnemonicWithPassphrase = mnemonicWithPassphrase,
                        accounts = accounts
                    )
                    sendEvent(Event.RecoverComplete)
                }
            }

            null -> {}
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

    fun startRecoveryScan() {
        runningScan?.cancel()
        runningScan = viewModelScope.launch {
            state.value.recoveryFactorSource?.let { recoveryFS ->
                recoverAccountsForFactorSourceUseCase(recoveryFS)
                    .onSuccess { accounts ->
                        if (isActive) {
                            deriveStateFromRecoveredAccounts(accounts)
                        }
                    }
                    .onFailure { failure ->
                        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(failure)) }
                        sendEvent(Event.CloseScan)
                    }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            sendEvent(Event.OnBackClick)
            _state.update { State() }
        }
    }

    private fun deriveStateFromRecoveredAccounts(accounts: List<AccountWithOnLedgerStatus>) {
        val allRecoveredAccounts = state.value.recoveredAccounts + accounts
        val activeAccounts = allRecoveredAccounts.filter {
            it.status == AccountWithOnLedgerStatus.Status.Active
        }.map { it.account }.toPersistentList()
        val maxActiveIndex = allRecoveredAccounts.indexOfLast { it.status == AccountWithOnLedgerStatus.Status.Active }
        val inactiveAccounts = if (maxActiveIndex == -1) {
            persistentListOf()
        } else {
            allRecoveredAccounts.subList(0, maxActiveIndex).filter { it.status == AccountWithOnLedgerStatus.Status.Inactive }
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

    data class State(
        val recoveryFactorSource: RecoveryFactorSource? = null,
        val contentState: ContentState = ContentState.ScanInProgress,
        val uiMessage: UiMessage? = null,
        val recoveredAccounts: ImmutableList<AccountWithOnLedgerStatus> = persistentListOf(),
        val interactionState: InteractionState? = null,
        val activeAccounts: PersistentList<Network.Account> = persistentListOf(),
        val inactiveAccounts: PersistentList<Selectable<Network.Account>> = persistentListOf(),
        val isRestoring: Boolean = false
    ) : UiState {

        enum class ContentState {
            ScanInProgress,
            ScanComplete
        }
    }
}

sealed interface RecoveryFactorSource {
    data class VirtualDeviceFactorSource(
        val virtualDeviceFactorSource: DeviceFactorSource,
        val mnemonicWithPassphrase: MnemonicWithPassphrase
    ) : RecoveryFactorSource

    data class Device(val factorSource: DeviceFactorSource, val mnemonicWithPassphrase: MnemonicWithPassphrase, val isOlympia: Boolean) :
        RecoveryFactorSource

    data class Ledger(val factorSource: LedgerHardwareWalletFactorSource, val isOlympia: Boolean) : RecoveryFactorSource

    val factorSourceId: FactorSource.FactorSourceID
        get() =
            when (this) {
                is VirtualDeviceFactorSource -> virtualDeviceFactorSource.id
                is Device -> factorSource.id
                is Ledger -> factorSource.id
            }

    val derivationPathScheme: DerivationPathScheme
        get() =
            when (this) {
                is VirtualDeviceFactorSource -> DerivationPathScheme.CAP_26
                is Device -> if (isOlympia) DerivationPathScheme.BIP_44_OLYMPIA else DerivationPathScheme.CAP_26
                is Ledger -> if (isOlympia) DerivationPathScheme.BIP_44_OLYMPIA else DerivationPathScheme.CAP_26
            }

}

sealed interface Event : OneOffEvent {
    data object CloseScan : Event
    data object RecoverComplete : Event
    data object OnBackClick : Event
}
