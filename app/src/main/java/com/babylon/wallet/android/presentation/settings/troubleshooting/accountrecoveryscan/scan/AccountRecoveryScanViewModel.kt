package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.ResolveDerivationPathsForRecoveryScanUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivePublicKeysSource
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeySpace
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.OnLedgerSettings
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.accountRecoveryScanned
import com.radixdlt.sargon.extensions.displayString
import com.radixdlt.sargon.extensions.indexInGlobalKeySpace
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.lastPathComponent
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.toFactorSourceId
import rdx.works.core.then
import rdx.works.profile.domain.AddRecoveredAccountsToProfileUseCase
import rdx.works.profile.domain.DeriveProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class AccountRecoveryScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val deriveProfileUseCase: DeriveProfileUseCase,
    private val addRecoveredAccountsToProfileUseCase: AddRecoveredAccountsToProfileUseCase,
    private val appEventBus: AppEventBus,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository,
    private val resolveDerivationPathsForRecoveryScanUseCase: ResolveDerivationPathsForRecoveryScanUseCase,
    private val sargonOsManager: SargonOsManager,
) : StateViewModel<AccountRecoveryScanViewModel.State>(),
    OneOffEventHandler<AccountRecoveryScanViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AccountRecoveryScanArgs(savedStateHandle)

    private var nextDerivationPathIndex: HdPathComponent = HdPathComponent.init(
        localKeySpace = 0u,
        keySpace = KeySpace.Unsecurified(isHardened = true)
    )

    override fun initialState(): State = State(
        isOlympiaSeedPhrase = args.isOlympia == true
    )

    init {
        viewModelScope.launch {
            val recoverySource = if (args.factorSourceId != null) {
                DerivePublicKeysSource.FactorSource(args.factorSourceId.value)
            } else {
                val mnemonic = accessFactorSourcesProxy.getTempMnemonicWithPassphrase() ?: error("No mnemonic provided")
                DerivePublicKeysSource.Mnemonic(mnemonic)
            }

            scan(recoverySource)
        }
    }

    fun onScanMoreClick() {
        val recoverySource = _state.value.recoverySource ?: return

        viewModelScope.launch {
            scan(recoverySource)
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun scan(recoverySource: DerivePublicKeysSource) {
        viewModelScope.launch {
            _state.update { it.copy(recoverySource = recoverySource, isScanningNetwork = true) }

            val pathsToResolve = resolveDerivationPathsForRecoveryScanUseCase(
                source = recoverySource,
                isOlympia = _state.value.isOlympiaSeedPhrase,
                currentPathIndex = nextDerivationPathIndex
            ).apply {
                this.derivationPaths.forEachIndexed { index, derivationPath ->
                    Timber.tag("Bakos").d("[$index]: ${derivationPath.displayString}")
                }
            }

            runCatching {
                sargonOsManager.sargonOs.derivePublicKeys(
                    derivationPaths = pathsToResolve.derivationPaths,
                    source = recoverySource
                )
            }.then { hdPublicKeys ->
                deriveAccounts(
                    networkId = pathsToResolve.networkId,
                    source = recoverySource,
                    derivedKeys = hdPublicKeys
                )
            }.onSuccess { accountsWithOnLedgerStatus ->
                val allRecoveredAccounts = state.value.recoveredAccounts + accountsWithOnLedgerStatus
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

                nextDerivationPathIndex = pathsToResolve.nextIndex

                _state.update { state ->
                    state.copy(
                        contentState = State.ContentState.ScanComplete,
                        recoveredAccounts = allRecoveredAccounts.toPersistentList(),
                        activeAccounts = activeAccounts,
                        inactiveAccounts = inactiveAccounts.map { Selectable(it) }.toPersistentList(),
                        isScanningNetwork = false
                    )
                }
            }.onFailure { error ->
                if (error is CommonException.HostInteractionAborted) {
                    _state.update { state -> state.copy(isScanningNetwork = false) }
                } else {
                    _state.update { state -> state.copy(isScanningNetwork = false, uiMessage = UiMessage.ErrorMessage(error)) }
                    delay(Constants.SNACKBAR_SHOW_DURATION_MS)
                    sendEvent(Event.CloseScan)
                }
            }
        }
    }

    private suspend fun deriveAccounts(
        networkId: NetworkId,
        source: DerivePublicKeysSource,
        derivedKeys: List<HierarchicalDeterministicPublicKey>
    ): Result<List<AccountWithOnLedgerStatus>> = runCatching {
        val factorSourceId = source.toFactorSourceId()

        derivedKeys.map { hdPublicKey ->
            Account.initBabylon(
                networkId = networkId,
                displayName = DisplayName.init(Constants.DEFAULT_ACCOUNT_NAME),
                hdPublicKey = hdPublicKey,
                factorSourceId = factorSourceId,
                onLedgerSettings = OnLedgerSettings(thirdPartyDeposits = ThirdPartyDeposits.accountRecoveryScanned())
            )
        }
    }.then { derivedAccounts ->
        resolveAccountsLedgerStateRepository(derivedAccounts)
    }

    @Suppress("UnsafeCallOnNullableType")
    fun onContinueClick() {
        val source = _state.value.recoverySource ?: return
        viewModelScope.launch {
            _state.update { it.copy(isScanningNetwork = true) }
            val accountsToRecover = state.value.activeAccounts +
                state.value.inactiveAccounts.filter { it.selected }.map { it.data }
            when (source) {
                is DerivePublicKeysSource.Mnemonic -> {
                    deriveProfileUseCase(
                        mnemonicWithPassphrase = source.v1,
                        accounts = Accounts(accountsToRecover)
                    ).onSuccess {
                        _state.update { state -> state.copy(isScanningNetwork = false) }
                        sendEvent(Event.RecoverComplete)
                    }.onFailure { error ->
                        if (error is CommonException.SecureStorageWriteException) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        } else {
                            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                        }
                        _state.update { it.copy(isScanningNetwork = false) }
                    }
                }
                is DerivePublicKeysSource.FactorSource -> {
                    if (accountsToRecover.isNotEmpty()) {
                        addRecoveredAccountsToProfileUseCase(accounts = accountsToRecover)
                    }

                    _state.update { it.copy(isScanningNetwork = false) }
                    sendEvent(Event.RecoverComplete)
                }
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
        val recoverySource: DerivePublicKeysSource? = null,
        val isOlympiaSeedPhrase: Boolean = false,
        val contentState: ContentState = ContentState.ScanInProgress,
        val recoveredAccounts: ImmutableList<AccountWithOnLedgerStatus> = persistentListOf(),
        val activeAccounts: PersistentList<Account> = persistentListOf(),
        val inactiveAccounts: PersistentList<Selectable<Account>> = persistentListOf(),
        val isScanningNetwork: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isLedgerFactorSource: Boolean
            get() = recoverySource?.toFactorSourceId()?.value?.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET

        enum class ContentState {
            ScanInProgress,
            ScanComplete
        }
    }

    sealed interface Event : OneOffEvent {
        data object CloseScan : Event
        data object RecoverComplete : Event
        data object OnBackClick : Event
    }

    private data class PathsToResolve(
        val derivationPaths: List<DerivationPath>,
        val networkId: NetworkId
    ) {

        val nextIndex: HdPathComponent
            get() {
                val nextIndexInGlobalKeySpace = if (derivationPaths.isNotEmpty()) {
                    derivationPaths.maxOf { it.lastPathComponent.indexInGlobalKeySpace } + 1u
                } else {
                    0u
                }

                return HdPathComponent.init(globalKeySpace = nextIndexInGlobalKeySpace)
            }
    }
}
