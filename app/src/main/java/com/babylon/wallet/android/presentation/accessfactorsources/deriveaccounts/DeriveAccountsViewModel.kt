package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput.ToReDeriveAccounts
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts.DeriveAccountsViewModel.DeriveAccountsUiState.ShowContentForFactorSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeySpace
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.OnLedgerSettings
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.extensions.accountRecoveryScanned
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.indexInGlobalKeySpace
import com.radixdlt.sargon.extensions.indexInLocalKeySpace
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.derivationPathEntityIndex
import rdx.works.core.sargon.derivationPathScheme
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.orDefault
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class DeriveAccountsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val ledgerMessenger: LedgerMessenger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StateViewModel<DeriveAccountsViewModel.DeriveAccountsUiState>(),
    OneOffEventHandler<DeriveAccountsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DeriveAccountsUiState = DeriveAccountsUiState()

    private lateinit var input: ToReDeriveAccounts
    private var profile: Profile? = null
    private var nextDerivationPathOffset: HdPathComponent = HdPathComponent.init(
        localKeySpace = 0u,
        keySpace = KeySpace.Unsecurified(isHardened = true)
    )
    private var reDerivePublicKeyJob: Job? = null

    init {
        reDerivePublicKeyJob = viewModelScope.launch {
            profile = getProfileUseCase.finishedOnboardingProfile()

            input = accessFactorSourcesIOHandler.getInput() as ToReDeriveAccounts
            nextDerivationPathOffset = input.nextDerivationPathIndex
            // if it is with given mnemonic it means it is an account recovery scan from onboarding,
            // thus profile is not initialized yet
            if (input is ToReDeriveAccounts.WithGivenMnemonic) {
                recoverAccountsForGivenMnemonic()
                sendEvent(Event.DerivingAccountsCompleted)
            } else { // else is with a given factor source
                when (input.factorSource) {
                    is FactorSource.Device -> sendEvent(Event.RequestBiometricPrompt) // request biometric auth
                    is FactorSource.Ledger -> initRecoveryFromLedgerFactorSource()
                    else -> {
                        Timber.w("FactorSourceKind ${input.factorSource.kind} not supported.")
                    }
                }
            }
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            when (val input = accessFactorSourcesIOHandler.getInput()) {
                is ToReDeriveAccounts -> {
                    when (input) {
                        is ToReDeriveAccounts.WithGivenFactorSource -> {
                            recoverAccountsForGivenFactorSource()
                                .onSuccess {
                                    sendEvent(Event.DerivingAccountsCompleted)
                                }
                                .onFailure { e ->
                                    if (e is ProfileException) {
                                        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.Failure(e))
                                        sendEvent(Event.DerivingAccountsCompleted)
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

    fun onUserDismiss() {
        viewModelScope.launch {
            sendEvent(Event.UserDismissed) // one to dismiss the dialog
            sendEvent(Event.UserDismissed) // one to dismiss the screen
        }
    }

    fun onRetryClick() {
        reDerivePublicKeyJob?.cancel()
        reDerivePublicKeyJob = viewModelScope.launch {
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
                Timber.d(it)
            }
    }

    private suspend fun recoverAccountsForGivenMnemonic() {
        _state.update { uiState ->
            uiState.copy(isFromOnboarding = true)
        }
        withContext(ioDispatcher) {
            val networkId = profile?.currentGateway?.network?.id.orDefault()
            val indicesToScan = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP44_OLYMPIA else DerivationPathScheme.CAP26,
                forNetworkId = networkId,
                factorSource = input.factorSource
            )
            val derivationPathsWithPublicKeys = reDerivePublicKeysWithGivenMnemonic(
                accountIndices = indicesToScan,
                forNetworkId = networkId
            )
            val derivedAccounts = deriveAccounts(
                hdPublicKeys = derivationPathsWithPublicKeys,
                forNetworkId = networkId
            )

            accessFactorSourcesIOHandler.setOutput(
                output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                    derivedAccounts = derivedAccounts,
                    nextDerivationPathIndex = HdPathComponent.init(indicesToScan.last().indexInGlobalKeySpace + 1u)
                )
            )
        }
    }

    private suspend fun recoverAccountsForGivenFactorSource(): Result<Unit> {
        return withContext(ioDispatcher) {
            val networkId = profile?.currentGateway?.network?.id.orDefault()

            val indicesToScan = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP44_OLYMPIA else DerivationPathScheme.CAP26,
                forNetworkId = networkId,
                factorSource = input.factorSource
            )
            reDerivePublicKeysWithGivenAccountIndices(
                accountIndices = indicesToScan,
                forNetworkId = networkId
            ).mapCatching { derivationPathsWithPublicKeys ->
                val derivedAccounts = deriveAccounts(
                    hdPublicKeys = derivationPathsWithPublicKeys,
                    forNetworkId = networkId
                )
                accessFactorSourcesIOHandler.setOutput(
                    output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                        derivedAccounts = derivedAccounts,
                        nextDerivationPathIndex = HdPathComponent.init(indicesToScan.last().indexInGlobalKeySpace + 1u)
                    )
                )
            }
        }
    }

    private fun reDerivePublicKeysWithGivenMnemonic(
        accountIndices: LinkedHashSet<HdPathComponent>,
        forNetworkId: NetworkId
    ): List<HierarchicalDeterministicPublicKey> {
        val mnemonicWithPassphrase = (input as ToReDeriveAccounts.WithGivenMnemonic).mnemonicWithPassphrase

        return publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices
        ).map { derivationPath ->
            mnemonicWithPassphrase.derivePublicKey(path = derivationPath)
        }
    }

    private suspend fun reDerivePublicKeysWithGivenAccountIndices(
        accountIndices: LinkedHashSet<HdPathComponent>,
        forNetworkId: NetworkId
    ): Result<List<HierarchicalDeterministicPublicKey>> {
        val derivationPaths = publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices,
            isForLegacyOlympia = input.isForLegacyOlympia
        )

        return when (val factorSource = input.factorSource) {
            is FactorSource.Device -> {
                publicKeyProvider.derivePublicKeysDeviceFactorSource(
                    deviceFactorSource = factorSource,
                    derivationPaths = derivationPaths
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
                    derivePublicKeyResponse.publicKeysHex.map { derived ->
                        HierarchicalDeterministicPublicKey(
                            publicKey = PublicKey.init(derived.publicKeyHex),
                            derivationPath = derivationPaths.first { it.string == derived.derivationPath }
                        )
                    }
                }
            }

            else -> Result.failure(IllegalStateException("FactorSourceKind ${factorSource.kind} not supported."))
        }
    }

    private fun deriveAccounts(
        hdPublicKeys: List<HierarchicalDeterministicPublicKey>,
        forNetworkId: NetworkId
    ): List<Account> = hdPublicKeys.mapNotNull { hdPublicKey ->
        val factorSourceId = when (val factorSource = input.factorSource) {
            is FactorSource.Device -> factorSource.value.id.asGeneral()
            is FactorSource.Ledger -> factorSource.value.id.asGeneral()
            else -> {
                Timber.w("FactorSourceKind ${factorSource.kind} not supported.")
                null
            }
        }

        factorSourceId?.let {
            Account.initBabylon(
                networkId = forNetworkId,
                displayName = DisplayName(Constants.DEFAULT_ACCOUNT_NAME),
                hdPublicKey = hdPublicKey,
                factorSourceId = it,
                onLedgerSettings = OnLedgerSettings(thirdPartyDeposits = ThirdPartyDeposits.accountRecoveryScanned())
            )
        }
    }

    private fun computeIndicesToScan(
        derivationPathScheme: DerivationPathScheme,
        forNetworkId: NetworkId,
        factorSource: FactorSource
    ): LinkedHashSet<HdPathComponent> {
        val network = profile?.networks?.asIdentifiable()?.getBy(forNetworkId)
        val usedIndices = network?.accounts
            ?.filter { account ->
                account.factorSourceId == factorSource.id && account.derivationPathScheme == derivationPathScheme
            }
            ?.map { account ->
                account.derivationPathEntityIndex.indexInLocalKeySpace
            }
            ?.toSet().orEmpty()

        val indicesToScan = LinkedHashSet<HdPathComponent>()
        val startIndex = nextDerivationPathOffset.indexInLocalKeySpace
        var currentIndex = startIndex
        do {
            if (currentIndex !in usedIndices) {
                indicesToScan.add(
                    HdPathComponent.init(
                        localKeySpace = currentIndex,
                        keySpace = KeySpace.Unsecurified(isHardened = true)
                    )
                )
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
        val isFromOnboarding: Boolean = false
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
