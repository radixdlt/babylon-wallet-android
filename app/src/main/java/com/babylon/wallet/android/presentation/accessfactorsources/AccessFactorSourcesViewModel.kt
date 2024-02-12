package com.babylon.wallet.android.presentation.accessfactorsources

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput.PublicKeyAndDerivationPath
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesViewModel.AccessFactorSourcesUiState.ShowContentFor
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.extensions.mainBabylonFactorSource
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.repository.AccessFactorSourcesProvider
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class AccessFactorSourcesViewModel @Inject constructor(
    private val accessFactorSourcesProvider: AccessFactorSourcesProvider,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<AccessFactorSourcesViewModel.AccessFactorSourcesUiState>(),
    OneOffEventHandler<AccessFactorSourcesViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): AccessFactorSourcesUiState = AccessFactorSourcesUiState()

    init {
        viewModelScope.launch {
            sendEvent(Event.RequestBiometricPrompt)
        }
    }

    fun biometricAuthenticationCompleted(isAuthenticated: Boolean) {
        viewModelScope.launch {
            if (isAuthenticated) {
                _state.update { uiState ->
                    uiState.copy(isAccessingFactorSourceInProgress = true)
                }
                when (val input = accessFactorSourcesUiProxy.getInput()) {
                    AccessFactorSourcesInput.Init -> { /* do nothing */ }

                    is AccessFactorSourcesInput.ToDerivePublicKey -> {
                        derivePublicKey(input)
                    }

                    is AccessFactorSourcesInput.ToSign -> { /* TBD */ }
                }

                _state.update { uiState ->
                    uiState.copy(
                        isAccessingFactorSourceInProgress = false,
                        isAccessingFactorSourceCompleted = true
                    )
                }
            } else {
                biometricAuthenticationDismissed()
            }
        }
    }

    private suspend fun derivePublicKey(input: AccessFactorSourcesInput.ToDerivePublicKey) {
        val profile = ensureBabylonFactorSourceExistUseCase()

        if (input.factorSource == null) { // device factor source
            val deviceFactorSource = profile.mainBabylonFactorSource() ?: error("Babylon factor source is not present")
            derivePublicKeyFromDeviceFactorSource(
                forNetworkId = input.forNetworkId,
                deviceFactorSource = deviceFactorSource
            )
        } else { // ledger factor source
            val ledgerFactorSource = input.factorSource as LedgerHardwareWalletFactorSource
            _state.update { uiState ->
                uiState.copy(
                    showContentFor = ShowContentFor.Ledger(selectedLedgerDevice = ledgerFactorSource)
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
        deviceFactorSource: DeviceFactorSource
    ) {
        val derivationPath = accessFactorSourcesProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = deviceFactorSource
        )
        val compressedPublicKey = accessFactorSourcesProvider.derivePublicKeyForDeviceFactorSource(
            deviceFactorSource = deviceFactorSource,
            derivationPath = derivationPath
        )
        val output = PublicKeyAndDerivationPath(
            compressedPublicKey = compressedPublicKey,
            derivationPath = derivationPath
        )

        accessFactorSourcesUiProxy.setOutput(output)
    }

    private suspend fun derivePublicKeyFromLedgerFactorSource(
        forNetworkId: NetworkId,
        ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) {
        val derivationPath = accessFactorSourcesProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = ledgerFactorSource
        )
        ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(LedgerInteractionRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource = ledgerFactorSource)
        ).onSuccess { derivePublicKeyResponse ->
            val publicKey = derivePublicKeyResponse.publicKeysHex.first().publicKeyHex.decodeHex() // TODO CHECK ABOUT IT
            val publicKeyAndDerivationPath = PublicKeyAndDerivationPath(
                compressedPublicKey = publicKey,
                derivationPath = derivationPath
            )
            accessFactorSourcesUiProxy.setOutput(publicKeyAndDerivationPath)
        }.onFailure { error ->
            accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(error))
        }
    }

    private fun biometricAuthenticationDismissed() {
        viewModelScope.launch {
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.Failure(CancellationException("Authentication dismissed"))
            )
            accessFactorSourcesUiProxy.reset()
        }
    }

    data class AccessFactorSourcesUiState(
        val isAccessingFactorSourceInProgress: Boolean = false,
        val isAccessingFactorSourceCompleted: Boolean = false,
        val showContentFor: ShowContentFor = ShowContentFor.Device
    ) : UiState {

        sealed interface ShowContentFor {
            data object Device : ShowContentFor
            data class Ledger(val selectedLedgerDevice: LedgerHardwareWalletFactorSource) : ShowContentFor
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
    }
}
