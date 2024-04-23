package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput.ToReDeriveAccounts
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts.DeriveAccountsViewModel.DeriveAccountsUiState.ShowContentForFactorSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.getBy
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.derivationPathEntityIndex
import rdx.works.core.sargon.derivationPathScheme
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.orDefault
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class DeriveAccountsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val ledgerMessenger: LedgerMessenger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StateViewModel<DeriveAccountsViewModel.DeriveAccountsUiState>(),
    OneOffEventHandler<DeriveAccountsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DeriveAccountsUiState = DeriveAccountsUiState()

    private lateinit var input: ToReDeriveAccounts
    private var profile: Profile? = null
    private var nextDerivationPathOffset: UInt = 0u
    private var reDerivePublicKeyJob: Job? = null

    init {
        reDerivePublicKeyJob = viewModelScope.launch {
            profile = if (getProfileUseCase.isInitialized()) getProfileUseCase.flow.firstOrNull() else null

            input = accessFactorSourcesUiProxy.getInput() as ToReDeriveAccounts
            nextDerivationPathOffset = input.nextDerivationPathOffset
            // if it is with given mnemonic it means it is an account recovery scan from onboarding,
            // thus profile is not initialized yet
            if (input is ToReDeriveAccounts.WithGivenMnemonic) {
                recoverAccountsForGivenMnemonic()
                sendEvent(Event.DerivingAccountsCompleted)
            } else { // else is with a given factor source
                when (input.factorSource) {
                    is FactorSource.Device -> sendEvent(Event.RequestBiometricPrompt) // request biometric auth
                    is FactorSource.Ledger -> initRecoveryFromLedgerFactorSource()
                }
            }
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            when (val input = accessFactorSourcesUiProxy.getInput()) {
                is ToReDeriveAccounts -> {
                    when (input) {
                        is ToReDeriveAccounts.WithGivenFactorSource -> {
                            recoverAccountsForGivenFactorSource()
                                .onSuccess {
                                    sendEvent(Event.DerivingAccountsCompleted)
                                }
                                .onFailure { e ->
                                    if (e is ProfileException) {
                                        accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(e))
                                        sendEvent(Event.DerivingAccountsCompleted)
                                    } else {
                                        _state.update { uiState ->
                                            uiState.copy(shouldShowRetryButton = true)
                                        }
                                    }
                                }
                        }

                        else -> {
                            /* do nothing */
                        }
                    }
                }

                else -> {
                    /* do nothing */
                }
            }
        }
    }

    fun onBiometricAuthenticationDismiss() {
        // biometric prompt dismissed, but bottom dialog remains visible
        // therefore we show the retry button
        _state.update { uiState ->
            uiState.copy(shouldShowRetryButton = true)
        }
    }

    fun onUserDismiss() {
        viewModelScope.launch {
            sendEvent(Event.UserDismissed) // one to dismiss the dialog
            sendEvent(Event.UserDismissed) // one to dismiss the screen
        }
    }

    fun onRetryClick() {
        reDerivePublicKeyJob?.cancel()
        reDerivePublicKeyJob = viewModelScope.launch {
            _state.update { uiState ->
                uiState.copy(shouldShowRetryButton = false)
            }
            when (state.value.showContentForFactorSource) {
                ShowContentForFactorSource.Device -> sendEvent(Event.RequestBiometricPrompt)
                is ShowContentForFactorSource.Ledger -> initRecoveryFromLedgerFactorSource()
            }
        }
    }

    private suspend fun initRecoveryFromLedgerFactorSource() {
        _state.update { uiState ->
            val ledger = input.factorSource as FactorSource.Ledger
            uiState.copy(showContentForFactorSource = ShowContentForFactorSource.Ledger(selectedLedgerDevice = ledger))
        }

        recoverAccountsForGivenFactorSource()
            .onSuccess {
                sendEvent(Event.DerivingAccountsCompleted)
            }
            .onFailure {
                _state.update { uiState ->
                    uiState.copy(shouldShowRetryButton = true)
                }
            }
    }

    private suspend fun recoverAccountsForGivenMnemonic() {
        _state.update { uiState ->
            uiState.copy(isFromOnboarding = true)
        }
        withContext(ioDispatcher) {
            val networkId = profile?.currentGateway?.network?.id.orDefault()
            val indicesToScan: Set<HdPathComponent> = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP44_OLYMPIA else DerivationPathScheme.CAP26,
                forNetworkId = networkId,
                factorSource = input.factorSource as FactorSource
            )
            val derivationPathsWithPublicKeys = reDerivePublicKeysWithGivenMnemonic(
                accountIndices = indicesToScan,
                forNetworkId = networkId
            )
            val derivedAccounts = deriveAccounts(
                derivationPathsWithPublicKeys = derivationPathsWithPublicKeys,
                forNetworkId = networkId
            )

            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                    derivedAccounts = derivedAccounts,
                    nextDerivationPathOffset = indicesToScan.last().value + 1u
                )
            )
        }
    }

    private suspend fun recoverAccountsForGivenFactorSource(): Result<Unit> {
        return withContext(ioDispatcher) {
            val networkId = profile?.currentGateway?.network?.id.orDefault()

            val indicesToScan: Set<HdPathComponent> = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP44_OLYMPIA else DerivationPathScheme.CAP26,
                forNetworkId = networkId,
                factorSource = input.factorSource as FactorSource
            )
            reDerivePublicKeysWithGivenAccountIndices(
                accountIndices = indicesToScan,
                forNetworkId = networkId
            ).mapCatching { derivationPathsWithPublicKeys ->
                val derivedAccounts = deriveAccounts(
                    derivationPathsWithPublicKeys = derivationPathsWithPublicKeys,
                    forNetworkId = networkId
                )
                accessFactorSourcesUiProxy.setOutput(
                    output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                        derivedAccounts = derivedAccounts,
                        nextDerivationPathOffset = indicesToScan.last().value + 1u
                    )
                )
            }
        }
    }

    private fun reDerivePublicKeysWithGivenMnemonic(
        accountIndices: Set<HdPathComponent>,
        forNetworkId: NetworkId
    ): Map<DerivationPath, PublicKey> {
        val derivationPaths = publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices
        )

        val derivationPathsWithPublicKeys = derivationPaths.associateWith { derivationPath ->
            (input as ToReDeriveAccounts.WithGivenMnemonic).mnemonicWithPassphrase
                .derivePublicKey(derivationPath = derivationPath)
        }

        return derivationPathsWithPublicKeys
    }

    private suspend fun reDerivePublicKeysWithGivenAccountIndices(
        accountIndices: Set<HdPathComponent>,
        forNetworkId: NetworkId
    ): Result<Map<DerivationPath, PublicKey>> {
        val derivationPaths = publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices,
            isForLegacyOlympia = input.isForLegacyOlympia
        )

        return when (val factorSource = input.factorSource) {
            is FactorSource.Device -> {
                publicKeyProvider.derivePublicKeysDeviceFactorSource(
                    deviceFactorSource = factorSource,
                    derivationPaths = derivationPaths,
                    isForLegacyOlympia = input.isForLegacyOlympia
                )
            }

            is FactorSource.Ledger -> {
                ledgerMessenger.sendDerivePublicKeyRequest(
                    interactionId = UUIDGenerator.uuid().toString(),
                    keyParameters = derivationPaths.map { derivationPath ->
                        LedgerInteractionRequest.KeyParameters(
                            curve = if (input.isForLegacyOlympia) Curve.Secp256k1 else Curve.Curve25519,
                            derivationPath = derivationPath.string
                        )
                    },
                    ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(factorSource = factorSource)
                ).mapCatching { derivePublicKeyResponse ->
                    val publicKeys = derivePublicKeyResponse.publicKeysHex
                    publicKeys.associate { publicKey ->
                        val derivationPath = derivationPaths.first { it.string == publicKey.derivationPath }
                        derivationPath to PublicKey.init(publicKey.publicKeyHex)
                    }
                }
            }
        }
    }

    private fun deriveAccounts(
        derivationPathsWithPublicKeys: Map<DerivationPath, PublicKey>,
        forNetworkId: NetworkId
    ): List<Account> = derivationPathsWithPublicKeys.entries.map { entry ->
        val factorSourceId = when (val factorSource = input.factorSource) {
            is FactorSource.Device -> factorSource.value.id.asGeneral()
            is FactorSource.Ledger -> factorSource.value.id.asGeneral()
        }
        Account.initBabylon(
            networkId = forNetworkId,
            displayName = DisplayName(Constants.DEFAULT_ACCOUNT_NAME),
            publicKey = entry.value,
            derivationPath = entry.key,
            factorSourceId = factorSourceId, // TODO integration appearance Id might be wrong
        )
    }

    private fun computeIndicesToScan(
        derivationPathScheme: DerivationPathScheme,
        forNetworkId: NetworkId,
        factorSource: FactorSource
    ): Set<HdPathComponent> {
        val network = profile?.networks?.getBy(forNetworkId)
        val usedIndices = network?.accounts()
            ?.filter { account ->
                account.factorSourceId == factorSource.id && account.derivationPathScheme == derivationPathScheme
            }
            ?.map { account ->
                account.derivationPathEntityIndex
            }
            ?.toSet().orEmpty()

        val indicesToScan = mutableSetOf<HdPathComponent>()
        val startIndex = nextDerivationPathOffset
        var currentIndex = startIndex
        do {
            if (currentIndex !in usedIndices) {
                indicesToScan.add(HdPathComponent(currentIndex))
            }
            currentIndex++
        } while (indicesToScan.size < ACCOUNTS_PER_SCAN)
        return indicesToScan
    }

    override fun onCleared() {
        super.onCleared()
        reDerivePublicKeyJob?.cancel()
    }

    data class DeriveAccountsUiState(
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.Device,
        val isFromOnboarding: Boolean = false,
        val shouldShowRetryButton: Boolean = false
    ) : UiState {

        sealed interface ShowContentForFactorSource {
            data object Device : ShowContentForFactorSource
            data class Ledger(val selectedLedgerDevice: FactorSource.Ledger) : ShowContentForFactorSource
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event

        data object DerivingAccountsCompleted : Event

        data object UserDismissed : Event
    }

    companion object {
        const val ACCOUNTS_PER_SCAN = 50
    }
}
