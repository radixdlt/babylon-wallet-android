package com.babylon.wallet.android.presentation.accessfactorsources

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceOutput.PublicKeyAndDerivationPath
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceViewModel.AccessFactorSourceUiState.ShowContentFor
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
import rdx.works.profile.data.repository.AccessFactorSourceProvider
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import javax.inject.Inject

@HiltViewModel
class AccessFactorSourcesViewModel @Inject constructor(
    private val accessFactorSourceProvider: AccessFactorSourceProvider,
    private val accessFactorSourceUiProxy: AccessFactorSourceUiProxy,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<AccessFactorSourcesViewModel.AccessFactorSourceUiState>() {

    override fun initialState(): AccessFactorSourceUiState = AccessFactorSourceUiState()

    fun biometricAuthenticationCompleted(isAuthenticated: Boolean) {
        viewModelScope.launch {
            if (isAuthenticated) {
                _state.update { uiState ->
                    uiState.copy(isAccessingFactorSourceInProgress = true)
                }
                when (val input = accessFactorSourceUiProxy.getInput()) {
                    AccessFactorSourceInput.Init -> { /* do nothing */ }

                    is AccessFactorSourceInput.ToCreateAccount -> {
                        derivePublicKey(input)
                    }

                    is AccessFactorSourceInput.ToSign -> { /* TBD */ }
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

    private suspend fun derivePublicKey(input: AccessFactorSourceInput.ToCreateAccount) {
        if (input.factorSource == null) { // device factor source
            val profile = ensureBabylonFactorSourceExistUseCase()
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
        val derivationPath = accessFactorSourceProvider.getDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = deviceFactorSource
        )
        val compressedPublicKey = accessFactorSourceProvider.derivePublicKeyForDeviceFactorSource(
            deviceFactorSource = deviceFactorSource,
            derivationPath = derivationPath
        )
        val output = PublicKeyAndDerivationPath(
            compressedPublicKey = compressedPublicKey,
            derivationPath = derivationPath
        )

        accessFactorSourceUiProxy.setOutput(output)
    }

    private suspend fun derivePublicKeyFromLedgerFactorSource(
        forNetworkId: NetworkId,
        ledgerFactorSource: LedgerHardwareWalletFactorSource
    ) {
        val derivationPath = accessFactorSourceProvider.getDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = ledgerFactorSource
        )
        val derivePublicKeyResponse = ledgerMessenger.sendDerivePublicKeyRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            keyParameters = listOf(LedgerInteractionRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
            ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource = ledgerFactorSource)
        )

        if (derivePublicKeyResponse.isSuccess) {
            val publicKeyResponse = derivePublicKeyResponse.getOrNull()
            if (publicKeyResponse == null) {
                accessFactorSourceUiProxy.setOutput(null)
                return
            }

            val publicKey = publicKeyResponse.publicKeysHex.first().publicKeyHex.decodeHex() // TODO CHECK ABOUT IT
            val publicKeyAndDerivationPath = PublicKeyAndDerivationPath(
                compressedPublicKey = publicKey,
                derivationPath = derivationPath
            )
            accessFactorSourceUiProxy.setOutput(publicKeyAndDerivationPath)
        } else {
            accessFactorSourceUiProxy.setOutput(null)
        }
    }

    private fun biometricAuthenticationDismissed() {
        viewModelScope.launch {
            accessFactorSourceUiProxy.setOutput(null)
            accessFactorSourceUiProxy.reset()
        }
    }

    data class AccessFactorSourceUiState(
        val isAccessingFactorSourceInProgress: Boolean = false,
        val isAccessingFactorSourceCompleted: Boolean = false,
        val showContentFor: ShowContentFor = ShowContentFor.Device
    ) : UiState {

        sealed interface ShowContentFor {
            data object Device : ShowContentFor
            data class Ledger(val selectedLedgerDevice: LedgerHardwareWalletFactorSource) : ShowContentFor
        }
    }
}
