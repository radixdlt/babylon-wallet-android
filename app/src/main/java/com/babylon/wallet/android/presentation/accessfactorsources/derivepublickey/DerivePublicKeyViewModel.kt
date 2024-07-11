package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput.HDPublicKey
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey.DerivePublicKeyViewModel.DerivePublicKeyUiState.ContentType
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.EntityKind
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
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DerivePublicKeyViewModel @Inject constructor(
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<DerivePublicKeyViewModel.DerivePublicKeyUiState>(),
    OneOffEventHandler<DerivePublicKeyViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DerivePublicKeyUiState = DerivePublicKeyUiState()

    private lateinit var input: AccessFactorSourcesInput.ToDerivePublicKey
    private var createEntityJob: Job? = null

    init {
        createEntityJob = viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToDerivePublicKey
            when (val factorSource = input.factorSource) {
                is FactorSource.Ledger -> {
                    _state.update { uiState ->
                        uiState.copy(
                            contentType = ContentType.ForLedgerAccount(selectedLedgerDevice = factorSource)
                        )
                    }
                    derivePublicKey().onSuccess {
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }
                }

                is FactorSource.Device -> {
                    _state.update { uiState ->
                        uiState.copy(
                            contentType = when (input.entityKind) {
                                EntityKind.ACCOUNT -> ContentType.ForDeviceAccount
                                EntityKind.PERSONA -> ContentType.ForPersona
                            }
                        )
                    }
                    if (input.isBiometricsProvided) {
                        biometricAuthenticationCompleted()
                    } else {
                        sendEvent(Event.RequestBiometricPrompt)
                    }
                }
            }
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            derivePublicKey().onSuccess {
                sendEvent(Event.AccessingFactorSourceCompleted)
            }.onFailure { e ->
                handleFailure(e)
            }
        }
    }

    private suspend fun DerivePublicKeyViewModel.handleFailure(e: Throwable) {
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
            when (state.value.contentType) {
                ContentType.ForPersona,
                ContentType.ForDeviceAccount -> {
                    sendEvent(Event.RequestBiometricPrompt)
                }

                is ContentType.ForLedgerAccount -> {
                    derivePublicKey().onSuccess {
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

    private suspend fun derivePublicKey(): Result<Unit> {
        return when (val factorSource = input.factorSource) {
            is FactorSource.Device -> {
                derivePublicKeyFromDeviceFactorSource(
                    forNetworkId = input.forNetworkId,
                    deviceFactorSource = factorSource,
                    entityKind = input.entityKind
                )
            }

            is FactorSource.Ledger -> {
                derivePublicKeyFromLedgerFactorSource(
                    forNetworkId = input.forNetworkId,
                    ledgerFactorSource = factorSource
                )
            }
        }
    }

    private suspend fun derivePublicKeyFromDeviceFactorSource(
        forNetworkId: NetworkId,
        deviceFactorSource: FactorSource.Device,
        entityKind: EntityKind
    ): Result<Unit> {
        val derivationPath = publicKeyProvider.getNextDerivationPathForFactorSource(
            forNetworkId = forNetworkId,
            factorSource = deviceFactorSource,
            entityKind = entityKind
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

    data class DerivePublicKeyUiState(
        val contentType: ContentType = ContentType.ForDeviceAccount,
        val shouldShowRetryButton: Boolean = false
    ) : UiState {

        sealed interface ContentType {
            data object ForDeviceAccount : ContentType
            data object ForPersona : ContentType
            data class ForLedgerAccount(val selectedLedgerDevice: FactorSource.Ledger) : ContentType
        }
    }

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
        data object AccessingFactorSourceCompleted : Event
        data object UserDismissed : Event
    }
}
