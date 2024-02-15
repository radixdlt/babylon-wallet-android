package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput.PublicKeyAndDerivationPath
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey.DerivePublicKeyViewModel.DerivePublicKeyUiState.ShowContentFor
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
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import java.util.concurrent.CancellationException
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
                    is AccessFactorSourcesInput.ToDerivePublicKey -> {
                        derivePublicKey(input)
                    }

                    else -> { /* do nothing */ }
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
        val derivationPath = publicKeyProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = deviceFactorSource
        )
        val compressedPublicKey = publicKeyProvider.derivePublicKeyForDeviceFactorSource(
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
        val derivationPath = publicKeyProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = ledgerFactorSource
        )
        ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(LedgerInteractionRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource = ledgerFactorSource)
        ).onSuccess { derivePublicKeyResponse ->
            val publicKey = derivePublicKeyResponse.publicKeysHex.first().publicKeyHex.decodeHex()
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
        }
    }

    data class DerivePublicKeyUiState(
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
