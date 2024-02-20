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

    init {
        viewModelScope.launch {
            sendEvent(Event.RequestBiometricPrompt)
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToDerivePublicKey
            derivePublicKey()
                .onSuccess {
                    // derivation is done so update UI
                    _state.update { uiState ->
                        uiState.copy(
                            isAccessingFactorSourceInProgress = false,
                            isAccessingFactorSourceCompleted = true
                        )
                    }
                }
                .onFailure {
                    _state.update { uiState ->
                        uiState.copy(
                            isAccessingFactorSourceInProgress = false,
                            shouldShowRetryButton = true
                        )
                    }
                }
        }
    }

    fun onBiometricAuthenticationDismiss() {
        _state.update { uiState ->
            uiState.copy(
                isAccessingFactorSourceInProgress = false,
                shouldShowRetryButton = true
            )
        }
    }

    fun onRetryClick() {
        viewModelScope.launch {
            _state.update { uiState ->
                uiState.copy(shouldShowRetryButton = false)
            }
            when (state.value.showContentForFactorSource) {
                ShowContentForFactorSource.Device -> sendEvent(Event.RequestBiometricPrompt)
                is ShowContentForFactorSource.Ledger -> {
                    derivePublicKey()
                        .onSuccess {
                            // derivation is done so update UI
                            _state.update { uiState ->
                                uiState.copy(
                                    isAccessingFactorSourceInProgress = false,
                                    isAccessingFactorSourceCompleted = true
                                )
                            }
                        }
                        .onFailure {
                            _state.update { uiState ->
                                uiState.copy(
                                    isAccessingFactorSourceInProgress = false,
                                    shouldShowRetryButton = true
                                )
                            }
                        }
                }
            }
        }
    }

    private suspend fun derivePublicKey(): Result<Unit> {
        _state.update { uiState ->
            uiState.copy(isAccessingFactorSourceInProgress = true)
        }

        val profile = ensureBabylonFactorSourceExistUseCase()

        return if (input.factorSource == null) { // device factor source
            val deviceFactorSource = profile.mainBabylonFactorSource() ?: error("Babylon factor source is not present")
            derivePublicKeyFromDeviceFactorSource(
                forNetworkId = input.forNetworkId,
                deviceFactorSource = deviceFactorSource
            )
        } else { // ledger factor source
            val ledgerFactorSource = input.factorSource as LedgerHardwareWalletFactorSource
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
        deviceFactorSource: DeviceFactorSource
    ): Result<Unit> {
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
        return Result.success(Unit)
    }

    private suspend fun derivePublicKeyFromLedgerFactorSource(
        forNetworkId: NetworkId,
        ledgerFactorSource: LedgerHardwareWalletFactorSource
    ): Result<Unit> {
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
            return Result.success(Unit)
        }.onFailure { error -> // it failed for some reason to derive the public keys (e.g. lost link connection)
            return Result.failure(error)
        }
        return Result.failure(IOException("failed to derive public keys"))
    }

    data class DerivePublicKeyUiState(
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.Device,
        val isAccessingFactorSourceInProgress: Boolean = false,
        val isAccessingFactorSourceCompleted: Boolean = false,
        val shouldShowRetryButton: Boolean = false
    ) : UiState {

        sealed interface ShowContentForFactorSource {
            data object Device : ShowContentForFactorSource
            data class Ledger(val selectedLedgerDevice: LedgerHardwareWalletFactorSource) : ShowContentForFactorSource
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
    }
}
