package com.babylon.wallet.android.presentation.accessfactorsources.createentity

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput.HDPublicKey
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.accessfactorsources.createentity.CreateEntityViewModel.CreateEntityUiState.CreatedEntityType
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class CreateEntityViewModel @Inject constructor(
    private val createPersonaWithDeviceFactorSourceUseCase: CreatePersonaWithDeviceFactorSourceUseCase,
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<CreateEntityViewModel.CreateEntityUiState>(),
    OneOffEventHandler<CreateEntityViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): CreateEntityUiState = CreateEntityUiState()

    private lateinit var input: AccessFactorSourcesInput
    private var createEntityJob: Job? = null

    init {
        createEntityJob = viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput()
            when (val typedInput = input) {
                is AccessFactorSourcesInput.ToCreatePersona -> sendEvent(Event.RequestBiometricPrompt)
                is AccessFactorSourcesInput.ToCreateAccount -> {
                    when (typedInput.factorSource) {
                        is FactorSource.Ledger -> {
                            derivePublicKey(typedInput).onSuccess {
                                sendEvent(Event.AccessingFactorSourceCompleted)
                            }
                        }

                        is FactorSource.Device -> if (typedInput.isBiometricsProvided) {
                            biometricAuthenticationCompleted()
                        } else {
                            sendEvent(Event.RequestBiometricPrompt)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            when (val typedInput = input) {
                is AccessFactorSourcesInput.ToCreatePersona -> {
                    createPersonaWithDeviceFactorSourceUseCase(typedInput.displayName, typedInput.personaData).onSuccess { persona ->
                        accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.CreatedPersona(persona))
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }.onFailure { e ->
                        handleFailure(e)
                    }
                }

                is AccessFactorSourcesInput.ToCreateAccount -> {
                    derivePublicKey(typedInput).onSuccess {
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }.onFailure { e ->
                        handleFailure(e)
                    }
                }

                else -> {}
            }
        }
    }

    private suspend fun CreateEntityViewModel.handleFailure(e: Throwable) {
        when (e) {
            is ProfileException -> {
                accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(e))
                sendEvent(Event.AccessingFactorSourceCompleted)
            }

            else -> {
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
        createEntityJob?.cancel()
        createEntityJob = viewModelScope.launch {
            _state.update { uiState ->
                uiState.copy(shouldShowRetryButton = false)
            }
            when (state.value.createdEntityType) {
                CreatedEntityType.Persona,
                CreatedEntityType.DeviceAccount -> {
                    sendEvent(Event.RequestBiometricPrompt)
                }

                is CreatedEntityType.LedgerAccount -> {
                    derivePublicKey(input as AccessFactorSourcesInput.ToCreateAccount).onSuccess {
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }
                }
            }
        }
    }

    fun onUserDismiss() {
        viewModelScope.launch {
            createEntityJob?.cancel()
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.Failure(CancellationException("User cancelled"))
            )
            sendEvent(Event.UserDismissed)
        }
    }

    private suspend fun derivePublicKey(input: AccessFactorSourcesInput.ToCreateAccount): Result<Unit> {
        return when (val factorSource = input.factorSource) {
            is FactorSource.Device -> {
                derivePublicKeyFromDeviceFactorSource(
                    forNetworkId = input.forNetworkId,
                    deviceFactorSource = factorSource
                )
            }

            is FactorSource.Ledger -> {
                _state.update { uiState ->
                    uiState.copy(
                        createdEntityType = CreatedEntityType.LedgerAccount(selectedLedgerDevice = factorSource)
                    )
                }
                derivePublicKeyFromLedgerFactorSource(
                    forNetworkId = input.forNetworkId,
                    ledgerFactorSource = factorSource
                )
            }
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
        return publicKeyProvider.deriveHDPublicKeyForDeviceFactorSource(
            deviceFactorSource = deviceFactorSource,
            derivationPath = derivationPath
        ).mapCatching { hdPublicKey ->
            accessFactorSourcesUiProxy.setOutput(output = HDPublicKey(hdPublicKey))
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
            val hdPublicKey = HierarchicalDeterministicPublicKey(
                publicKey = PublicKey.init(derivePublicKeyResponse.publicKeysHex.first().publicKeyHex),
                derivationPath = derivationPath
            )
            accessFactorSourcesUiProxy.setOutput(HDPublicKey(hdPublicKey))
            return Result.success(Unit)
        }.onFailure { error -> // it failed for some reason to derive the public keys (e.g. lost link connection)
            return Result.failure(error)
        }
        return Result.failure(IOException("failed to derive public keys"))
    }

    data class CreateEntityUiState(
        val createdEntityType: CreatedEntityType = CreatedEntityType.DeviceAccount,
        val shouldShowRetryButton: Boolean = false
    ) : UiState {

        sealed interface CreatedEntityType {
            data object DeviceAccount : CreatedEntityType
            data object Persona : CreatedEntityType
            data class LedgerAccount(val selectedLedgerDevice: FactorSource.Ledger) : CreatedEntityType
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
        data object AccessingFactorSourceCompleted : Event
        data object UserDismissed : Event
    }
}
