package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
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
import com.radixdlt.extensions.removeLeadingZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.UUIDGenerator
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.createAccount
import rdx.works.profile.data.model.extensions.derivationPathEntityIndex
import rdx.works.profile.data.model.extensions.derivationPathScheme
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.initializeAccount
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.derivationPathEntityIndex
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject

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

    private var profile: Profile? = null
    private var nextDerivationPathOffset: Int = 0

    init {
        viewModelScope.launch {
            profile = if (getProfileUseCase.isInitialized()) getProfileUseCase.invoke().firstOrNull() else null

            val input = accessFactorSourcesUiProxy.getInput()
            nextDerivationPathOffset = (input as ToReDeriveAccounts).nextDerivationPathOffset
            // if it is with given mnemonic it means it is an account recovery scan from onboarding,
            // thus profile is not initialized yet
            if (input is ToReDeriveAccounts.WithGivenMnemonic) {
                recoverAccountsForGivenMnemonic(input = input)
                sendEvent(Event.DerivingAccountsCompleted)
            } else { // else is with a given factor source
                if (input.factorSource is LedgerHardwareWalletFactorSource) {
                    initRecoveryFromLedgerFactorSource(input)
                } else { // is DeviceFactorSource then request biometric auth
                    sendEvent(Event.RequestBiometricPrompt)
                }
            }
        }
    }

    fun biometricAuthenticationCompleted(isAuthenticated: Boolean) {
        viewModelScope.launch {
            if (isAuthenticated) {
                _state.update { uiState ->
                    uiState.copy(isDerivingAccountsInProgress = true)
                }
                when (val input = accessFactorSourcesUiProxy.getInput()) {
                    is ToReDeriveAccounts -> {
                        when (input) {
                            is ToReDeriveAccounts.WithGivenFactorSource -> {
                                recoverAccountsForGivenFactorSource(input = input)
                            }
                            else -> { /* do nothing */ }
                        }
                    }
                    else -> { /* do nothing */ }
                }

                _state.update { uiState ->
                    uiState.copy(isDerivingAccountsInProgress = false)
                }
                sendEvent(Event.DerivingAccountsCompleted)
            } else { // biometric auth dismissed
                viewModelScope.launch {
                    accessFactorSourcesUiProxy.setOutput(
                        output = AccessFactorSourcesOutput.Failure(CancellationException("Authentication dismissed"))
                    )
                }
            }
        }
    }

    fun onUserDismissed() {
        viewModelScope.launch {
            accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(CancellationException("User dismissed")))
            sendEvent(Event.UserDismissed)
        }
    }

    private suspend fun initRecoveryFromLedgerFactorSource(input: ToReDeriveAccounts) {
        _state.update { uiState ->
            val ledger = input.factorSource as LedgerHardwareWalletFactorSource
            uiState.copy(
                isDerivingAccountsInProgress = true,
                showContentForFactorSource = ShowContentForFactorSource.Ledger(selectedLedgerDevice = ledger)
            )
        }

        recoverAccountsForGivenFactorSource(input = input as ToReDeriveAccounts.WithGivenFactorSource)

        _state.update { uiState ->
            uiState.copy(isDerivingAccountsInProgress = false)
        }
        sendEvent(Event.DerivingAccountsCompleted)
    }

    private suspend fun recoverAccountsForGivenMnemonic(
        input: ToReDeriveAccounts.WithGivenMnemonic
    ) {
        _state.update { uiState ->
            uiState.copy(
                isFromOnboarding = true,
                isDerivingAccountsInProgress = true
            )
        }
        withContext(ioDispatcher) {
            val networkId = profile?.currentNetwork?.knownNetworkId ?: Radix.Gateway.mainnet.network.networkId()

            val indicesToScan: Set<Int> = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP_44_OLYMPIA else DerivationPathScheme.CAP_26,
                forNetworkId = networkId,
                factorSource = input.factorSource as FactorSource
            )
            val derivationPathsWithPublicKeys = reDerivePublicKeysWithGivenMnemonic(
                input = input,
                accountIndices = indicesToScan,
                forNetworkId = networkId
            )
            val derivedAccounts = deriveAccounts(
                input = input,
                derivationPathsWithPublicKeys = derivationPathsWithPublicKeys,
                forNetworkId = networkId
            )

            _state.update { uiState ->
                uiState.copy(
                    isFromOnboarding = false,
                    isDerivingAccountsInProgress = false
                )
            }
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                    derivedAccounts = derivedAccounts,
                    nextDerivationPathOffset = indicesToScan.last() + 1
                )
            )
        }
    }

    private suspend fun recoverAccountsForGivenFactorSource(
        input: ToReDeriveAccounts.WithGivenFactorSource
    ) {
        _state.update { uiState ->
            uiState.copy(isDerivingAccountsInProgress = true)
        }
        withContext(ioDispatcher) {
            val networkId = profile?.currentNetwork?.knownNetworkId ?: Radix.Gateway.mainnet.network.networkId()

            val indicesToScan: Set<Int> = computeIndicesToScan(
                derivationPathScheme = if (input.isForLegacyOlympia) DerivationPathScheme.BIP_44_OLYMPIA else DerivationPathScheme.CAP_26,
                forNetworkId = networkId,
                factorSource = input.factorSource as FactorSource
            )
            val derivationPathsWithPublicKeys = reDerivePublicKeysWithGivenAccountIndices(
                input = input,
                accountIndices = indicesToScan,
                forNetworkId = networkId
            )
            val derivedAccounts = deriveAccounts(
                input = input,
                derivationPathsWithPublicKeys = derivationPathsWithPublicKeys,
                forNetworkId = networkId
            )

            _state.update { uiState ->
                uiState.copy(isDerivingAccountsInProgress = false)
            }
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath(
                    derivedAccounts = derivedAccounts,
                    nextDerivationPathOffset = indicesToScan.last() + 1
                )
            )
        }
    }

    private fun reDerivePublicKeysWithGivenMnemonic(
        input: ToReDeriveAccounts.WithGivenMnemonic,
        accountIndices: Set<Int>,
        forNetworkId: NetworkId
    ): Map<DerivationPath, ByteArray> {
        val derivationPaths = publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices
        )
        val derivationPathsWithPublicKeys = derivationPaths.associateWith { derivationPath ->
            input.mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath).removeLeadingZero()
        }

        return derivationPathsWithPublicKeys
    }

    private suspend fun reDerivePublicKeysWithGivenAccountIndices(
        input: ToReDeriveAccounts.WithGivenFactorSource,
        accountIndices: Set<Int>,
        forNetworkId: NetworkId
    ): Map<DerivationPath, ByteArray> {
        val derivationPaths = publicKeyProvider.getDerivationPathsForIndices(
            forNetworkId = forNetworkId,
            indices = accountIndices,
            isForLegacyOlympia = input.isForLegacyOlympia
        )

        return when (input.factorSource) {
            is DeviceFactorSource -> {
                val derivationPathsWithPublicKeys = publicKeyProvider.derivePublicKeysDeviceFactorSource(
                    deviceFactorSource = input.factorSource,
                    derivationPaths = derivationPaths,
                    isForLegacyOlympia = input.isForLegacyOlympia
                )
                derivationPathsWithPublicKeys
            }

            is LedgerHardwareWalletFactorSource -> {
                val response = ledgerMessenger.sendDerivePublicKeyRequest(
                    interactionId = UUIDGenerator.uuid().toString(),
                    keyParameters = derivationPaths.map { derivationPath ->
                        LedgerInteractionRequest.KeyParameters(
                            curve = if (input.isForLegacyOlympia) Curve.Secp256k1 else Curve.Curve25519,
                            derivationPath = derivationPath.path
                        )
                    },
                    ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource = input.factorSource)
                )

                if (response.isSuccess) {
                    val publicKeys = response.getOrThrow().publicKeysHex
                    val derivationPathsWithPublicKeys = publicKeys.associate { publicKey ->
                        val derivationPath = derivationPaths.first { it.path == publicKey.derivationPath }
                        derivationPath to publicKey.publicKeyHex.decodeHex()
                    }
                    derivationPathsWithPublicKeys
                } else if (response.isFailure) {
                    accessFactorSourcesUiProxy.setOutput(
                        AccessFactorSourcesOutput.Failure(
                            error = response.exceptionOrNull() ?: IOException("unknown error")
                        )
                    )
                    emptyMap()
                } else {
                    accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(error = IOException("unknown error")))
                    emptyMap()
                }
            }
        }
    }

    private suspend fun deriveAccounts(
        input: ToReDeriveAccounts,
        derivationPathsWithPublicKeys: Map<DerivationPath, ByteArray>,
        forNetworkId: NetworkId
    ): List<Network.Account> {
        val profile = if (getProfileUseCase.isInitialized()) getProfileUseCase.invoke().firstOrNull() else null

        val derivedAccounts = derivationPathsWithPublicKeys.map { publicKey ->
            profile?.createAccount( // it is recovery from account settings (profile already exists)
                displayName = Constants.DEFAULT_ACCOUNT_NAME,
                onNetworkId = forNetworkId,
                compressedPublicKey = publicKey.value,
                derivationPath = publicKey.key,
                factorSource = input.factorSource,
                onLedgerSettings = Network.Account.OnLedgerSettings.init(),
                isForLegacyOlympia = input.isForLegacyOlympia,
                appearanceID = publicKey.key.derivationPathEntityIndex() % AccountGradientList.count()
            ) ?: initializeAccount( // it is recovery from on boarding
                displayName = Constants.DEFAULT_ACCOUNT_NAME,
                onNetworkId = forNetworkId,
                compressedPublicKey = publicKey.value,
                derivationPath = publicKey.key,
                factorSource = input.factorSource,
                onLedgerSettings = Network.Account.OnLedgerSettings.init()
            )
        }
        return derivedAccounts
    }

    private fun computeIndicesToScan(
        derivationPathScheme: DerivationPathScheme,
        forNetworkId: NetworkId,
        factorSource: FactorSource
    ): Set<Int> {
        val network = profile?.networks?.firstOrNull { it.networkID == forNetworkId.value }
        val usedIndices = network?.accounts
            ?.filter { account ->
                account.factorSourceId == factorSource.id && account.derivationPathScheme == derivationPathScheme
            }
            ?.map { account ->
                account.derivationPathEntityIndex
            }
            ?.toSet().orEmpty()

        val indicesToScan = mutableSetOf<Int>()
        val startIndex = nextDerivationPathOffset
        var currentIndex = startIndex
        do {
            if (currentIndex !in usedIndices) {
                indicesToScan.add(currentIndex)
            }
            currentIndex++
        } while (indicesToScan.size < ACCOUNTS_PER_SCAN)
        return indicesToScan
    }

    data class DeriveAccountsUiState(
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.Device,
        val isDerivingAccountsInProgress: Boolean = false,
        val isFromOnboarding: Boolean = false
    ) : UiState {

        sealed interface ShowContentForFactorSource {
            data object Device : ShowContentForFactorSource
            data class Ledger(val selectedLedgerDevice: LedgerHardwareWalletFactorSource) : ShowContentForFactorSource
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
