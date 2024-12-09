package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SecureStorageKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.driver.BiometricsFailure
import com.radixdlt.sargon.os.driver.BiometricsHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.data.repository.PublicKeyProvider
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DerivePublicKeysViewModel @Inject constructor(
    private val publicKeyProvider: PublicKeyProvider,
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val ledgerMessenger: LedgerMessenger,
    private val getProfileUseCase: GetProfileUseCase,
    private val biometricsHandler: BiometricsHandler
) : StateViewModel<DerivePublicKeysViewModel.State>(),
    OneOffEventHandler<DerivePublicKeysViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    private lateinit var input: AccessFactorSourcesInput.ToDerivePublicKeys
    private var derivePublicKeyJob: Job? = null

    init {
        derivePublicKeyJob = viewModelScope.launch {
            input = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToDerivePublicKeys

            val factorSource = getProfileUseCase().factorSourceById(input.factorSourceId.asGeneral()) ?: run {
                finishWithFailure(CommonException.SigningRejected(), input.factorSourceId)
                return@launch
            }

            _state.update {
                it.copy(content = State.Content.Resolved(purpose = input.purpose, factorSource = factorSource))
            }
            deriveKeys(factorSource)
        }
    }

    private suspend fun deriveKeys(factorSource: FactorSource) {
        when (factorSource) {
            is FactorSource.Device -> {
                derivePublicKeys(
                    deviceFactorSource = factorSource.value
                ).onSuccess { factorInstances ->
                    finishWithSuccess(factorInstances)
                }.onFailure {
                    finishWithFailure(it, factorSource.value.id)
                }
            }

            is FactorSource.Ledger -> {
                derivePublicKeys(
                    ledgerFactorSource = factorSource.value
                ).onSuccess { factorInstances ->
                    finishWithSuccess(factorInstances)
                }.onFailure {
                    finishWithFailure(it, factorSource.value.id)
                }
            }

            else -> {
                // Not supported yet
            }
        }
    }

    private suspend fun derivePublicKeys(
        deviceFactorSource: DeviceFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> = biometricsHandler.askForBiometrics()
        .mapCatching {
            input.derivationPaths.map { derivationPath ->
                publicKeyProvider.deriveHDPublicKeyForDeviceFactorSource(
                    deviceFactorSource = deviceFactorSource.asGeneral(),
                    derivationPath = derivationPath
                ).map { hdPublicKey ->
                    HierarchicalDeterministicFactorInstance(
                        factorSourceId = deviceFactorSource.id,
                        publicKey = hdPublicKey
                    )
                }.getOrThrow()
            }
        }

    private suspend fun derivePublicKeys(
        ledgerFactorSource: LedgerHardwareWalletFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        val factorInstances = input.derivationPaths.map { derivationPath ->
            ledgerMessenger.sendDerivePublicKeyRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                keyParameters = listOf(LedgerInteractionRequest.KeyParameters.from(derivationPath)),
                ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(
                    factorSource = ledgerFactorSource.asGeneral()
                )
            ).map { derivePublicKeyResponse ->
                HierarchicalDeterministicFactorInstance(
                    factorSourceId = ledgerFactorSource.id,
                    publicKey = HierarchicalDeterministicPublicKey(
                        publicKey = PublicKey.init(derivePublicKeyResponse.publicKeysHex.first().publicKeyHex),
                        derivationPath = derivationPath
                    )
                )
            }.getOrElse {
                return Result.failure(it)
            }
        }

        return Result.success(factorInstances)
    }

    private suspend fun finishWithSuccess(factorInstances: List<HierarchicalDeterministicFactorInstance>) {
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.DerivedPublicKeys(
                factorSourceId = input.factorSourceId,
                factorInstances = factorInstances
            )
        )
        sendEvent(Event.Dismiss)
    }

    private suspend fun finishWithFailure(
        error: Throwable,
        factorSourceId: FactorSourceIdFromHash
    ) {
        Timber.w(error, "Received error when deriving keys")
        val commonError = when (error) {
            // Error received from BiometricsHandler
            is BiometricsFailure -> error.toCommonException(SecureStorageKey.DeviceFactorSourceMnemonic(factorSourceId = factorSourceId))
            // Error received from MnemonicRepository
            is ProfileException.SecureStorageAccess -> CommonException.SecureStorageReadException()

            // TODO: Handle non fatal errors

            // Any other fatal error is resolved as rejected
            else -> CommonException.SigningRejected()
        }

        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.Failure(commonError))
        sendEvent(Event.Dismiss)
    }

    fun onRetryClick() {
        derivePublicKeyJob?.cancel()
        val factorSource = (state.value.content as? State.Content.Resolved)?.factorSource ?: return

        viewModelScope.launch {
            deriveKeys(factorSource = factorSource)
        }
    }

    fun onUserDismiss() {
        viewModelScope.launch {
            derivePublicKeyJob?.cancel()
            accessFactorSourcesIOHandler.setOutput(
                output = AccessFactorSourcesOutput.Failure(CommonException.SigningRejected()) // TODO
            )
            sendEvent(Event.Dismiss)
        }
    }

    data class State(
        val content: Content = Content.Resolving
    ) : UiState {

        sealed interface Content {
            data object Resolving : Content

            data class Resolved(
                val purpose: DerivationPurpose,
                val factorSource: FactorSource
            ) : Content
        }
    }

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
}
