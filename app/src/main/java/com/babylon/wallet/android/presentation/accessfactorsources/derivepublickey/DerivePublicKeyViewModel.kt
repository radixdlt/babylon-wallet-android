package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput.PublicKeyAndDerivationPath
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey.DerivePublicKeyViewModel.DerivePublicKeyUiState.ShowContentForFactorSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DerivePublicKeyViewModel @Inject constructor(
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<DerivePublicKeyViewModel.DerivePublicKeyUiState>(),
    OneOffEventHandler<DerivePublicKeyViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DerivePublicKeyUiState = DerivePublicKeyUiState()

    private lateinit var input: AccessFactorSourcesInput.ToDerivePublicKey
    private var derivePublicKeyJob: Job? = null

    init {
        derivePublicKeyJob = viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToDerivePublicKey
            when (input.factorSource) {
                is FactorSource.Ledger -> {
                    if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist()) {
                        derivePublicKey().onSuccess {
                            sendEvent(Event.AccessingFactorSourceCompleted)
                        }
                    } else {
                        // 1st account created with ledger, so we need to create BDFS too and authenticate first
                        sendEvent(Event.RequestBiometricPrompt)
                    }
                }

                is FactorSource.Device,
                null -> {
                    sendEvent(Event.RequestBiometricPrompt)
                }
            }
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToDerivePublicKey
            derivePublicKey()
                .onSuccess {
                    sendEvent(Event.AccessingFactorSourceCompleted)
                }
                .onFailure {
                    _state.update { uiState ->
                        uiState.copy(shouldShowRetryButton = true)
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

    fun onRetryClick() {
        derivePublicKeyJob?.cancel()
        derivePublicKeyJob = viewModelScope.launch {
            _state.update { uiState ->
                uiState.copy(shouldShowRetryButton = false)
            }
            when (state.value.showContentForFactorSource) {
                ShowContentForFactorSource.Device -> sendEvent(Event.RequestBiometricPrompt)
                is ShowContentForFactorSource.Ledger -> {
                    derivePublicKey().onSuccess {
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }
                }
            }
        }
    }

    private suspend fun derivePublicKey(): Result<Unit> {
        val profile = ensureBabylonFactorSourceExistUseCase()

        return if (input.factorSource == null) { // device factor source
            val deviceFactorSource = profile.mainBabylonFactorSource ?: error("Babylon factor source is not present")
            derivePublicKeyFromDeviceFactorSource(
                forNetworkId = input.forNetworkId,
                deviceFactorSource = deviceFactorSource.asGeneral()
            )
        } else { // ledger factor source
            val ledgerFactorSource = input.factorSource as FactorSource.Ledger
            _state.update { uiState ->
                uiState.copy(
                    showContentForFactorSource = ShowContentForFactorSource.Ledger(selectedLedgerDevice = ledgerFactorSource)
                )
            }
            derivePublicKeyFromLedgerFactorSource(
                forNetworkId = input.forNetworkId,
                ledgerFactorSource = ledgerFactorSource
            )
        }
    }

    private suspend fun derivePublicKeyFromDeviceFactorSource(
        forNetworkId: NetworkId,
        deviceFactorSource: FactorSource.Device
    ): Result<Unit> {
        val derivationPath = publicKeyProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = deviceFactorSource
        )
        return publicKeyProvider.derivePublicKeyForDeviceFactorSource(
            deviceFactorSource = deviceFactorSource,
            derivationPath = derivationPath
        ).mapCatching { publicKey ->
            accessFactorSourcesUiProxy.setOutput(output = PublicKeyAndDerivationPath(
                publicKey = publicKey,
                derivationPath = derivationPath
            ))
        }
    }

    private suspend fun derivePublicKeyFromLedgerFactorSource(
        forNetworkId: NetworkId,
        ledgerFactorSource: FactorSource.Ledger
    ): Result<Unit> {
        val derivationPath = publicKeyProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = ledgerFactorSource
        )
        ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(LedgerInteractionRequest.KeyParameters(Curve.Curve25519, derivationPath.string)),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(factorSource = ledgerFactorSource)
        ).onSuccess { derivePublicKeyResponse ->
            val publicKey = PublicKey.init(derivePublicKeyResponse.publicKeysHex.first().publicKeyHex)
            val publicKeyAndDerivationPath = PublicKeyAndDerivationPath(
                publicKey = publicKey,
                derivationPath = derivationPath
            )
            accessFactorSourcesUiProxy.setOutput(publicKeyAndDerivationPath)
            return Result.success(Unit)
        }.onFailure { error -> // it failed for some reason to derive the public keys (e.g. lost link connection)
            return Result.failure(error)
        }
        return Result.failure(IOException("failed to derive public keys"))
    }

    data class DerivePublicKeyUiState(
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.Device,
        val shouldShowRetryButton: Boolean = false
    ) : UiState {

        sealed interface ShowContentForFactorSource {
            data object Device : ShowContentForFactorSource
            data class Ledger(val selectedLedgerDevice: FactorSource.Ledger) : ShowContentForFactorSource
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
        data object AccessingFactorSourceCompleted : Event
    }
}
