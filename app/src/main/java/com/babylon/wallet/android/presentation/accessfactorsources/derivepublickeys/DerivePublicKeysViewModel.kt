package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceError
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessOffDeviceMnemonicFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.Event
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class DerivePublicKeysViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSource,
    getProfileUseCase: GetProfileUseCase,
) : StateViewModel<DerivePublicKeysViewModel.State>(),
    OneOffEventHandler<DerivePublicKeysViewModel.Event> by OneOffEventHandlerImpl() {



    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToDerivePublicKeys

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        id = proxyInput.request.factorSourceId.asGeneral(),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback
    )

    override fun initialState(): State = State(
        accessState = accessDelegate.state.value
    )

    init {
        accessDelegate
            .state
            .onEach { accessState ->
                _state.update { it.copy(accessState = accessState) }
            }
            .launchIn(viewModelScope)
    }

    fun onDismiss() = accessDelegate.onDismiss()

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) = accessDelegate.onSeedPhraseWordChanged(wordIndex, word)

    fun onPasswordTyped(password: String) = accessDelegate.onPasswordTyped(password)

    fun onRetry() = accessDelegate.onRetry()

    fun onMessageShown() = accessDelegate.onMessageShown()

    fun onInputConfirmed() = accessDelegate.onInputConfirmed()


    private suspend fun onAccess(factorSource: FactorSource): Result<Unit> {
        return Result.failure(CommonException.SigningRejected())
    }

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.DerivedPublicKeys.Rejected)
    }


//    private suspend fun deriveKeys(factorSource: FactorSource) {
//        when (factorSource) {
//            is FactorSource.Device -> {
//                derivePublicKeys(
//                    deviceFactorSource = factorSource.value
//                ).onSuccess { factorInstances ->
//                    finishWithSuccess(factorInstances)
//                }.onFailure {
//                    handleFailure(it, factorSource.value.id)
//                }
//            }
//
//            is FactorSource.Ledger -> {
//                derivePublicKeys(
//                    ledgerFactorSource = factorSource.value
//                ).onSuccess { factorInstances ->
//                    finishWithSuccess(factorInstances)
//                }.onFailure {
//                    handleFailure(it, factorSource.value.id)
//                }
//            }
//
//            else -> {
//                // Not supported yet
//            }
//        }
//    }

//    private suspend fun derivePublicKeys(
//        deviceFactorSource: DeviceFactorSource
//    ): Result<List<HierarchicalDeterministicFactorInstance>> = biometricsHandler.askForBiometrics()
//        .mapCatching {
//            input.request.derivationPaths.map { derivationPath ->
//                publicKeyProvider.deriveHDPublicKeyForDeviceFactorSource(
//                    deviceFactorSource = deviceFactorSource.asGeneral(),
//                    derivationPath = derivationPath
//                ).map { hdPublicKey ->
//                    HierarchicalDeterministicFactorInstance(
//                        factorSourceId = deviceFactorSource.id,
//                        publicKey = hdPublicKey
//                    )
//                }.getOrThrow()
//            }
//        }
//
//    private suspend fun derivePublicKeys(
//        ledgerFactorSource: LedgerHardwareWalletFactorSource
//    ): Result<List<HierarchicalDeterministicFactorInstance>> {
//        val factorInstances = input.request.derivationPaths.map { derivationPath ->
//            ledgerMessenger.sendDerivePublicKeyRequest(
//                interactionId = UUIDGenerator.uuid().toString(),
//                keyParameters = listOf(LedgerInteractionRequest.KeyParameters.from(derivationPath)),
//                ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(
//                    factorSource = ledgerFactorSource.asGeneral()
//                )
//            ).map { derivePublicKeyResponse ->
//                HierarchicalDeterministicFactorInstance(
//                    factorSourceId = ledgerFactorSource.id,
//                    publicKey = HierarchicalDeterministicPublicKey(
//                        publicKey = PublicKey.init(derivePublicKeyResponse.publicKeysHex.first().publicKeyHex),
//                        derivationPath = derivationPath
//                    )
//                )
//            }.getOrElse {
//                return Result.failure(it)
//            }
//        }
//
//        return Result.success(factorInstances)
//    }

    private suspend fun finishWithSuccess(factorInstances: List<HierarchicalDeterministicFactorInstance>) {
        sendEvent(Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.DerivedPublicKeys.Success(
                factorSourceId = proxyInput.request.factorSourceId,
                factorInstances = factorInstances
            )
        )
    }

    data class State(
        val accessState: AccessFactorSourceDelegate.State
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
