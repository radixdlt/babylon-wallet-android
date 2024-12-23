package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.CommonException.SecureStorageAccessException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.SecureStorageAccessErrorKind
import com.radixdlt.sargon.SecureStorageAccessErrorKind.USER_CANCELLED
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.Signable
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class GetSignaturesViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    @Suppress("UNCHECKED_CAST")
    private val input = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSign<Signable.Payload>

    private var signingJob: Job? = null

    override fun initialState(): State = State(
        signPurpose = input.purpose
    )

    init {
        viewModelScope.launch {
            resolveFactorSourcesAndSign()
        }
    }

    fun onDismiss() = viewModelScope.launch {
        signingJob?.cancel()

        val output = input.perFactorSource.map { input ->
            OutputPerFactorSource.Neglected<Signable.ID>(
                factorSourceId = input.factorSourceId,
                reason = NeglectFactorReason.USER_EXPLICITLY_SKIPPED
            )
        }

        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput(output))
    }

    fun onRetry() {
        val factorSources = _state.value.factorSources ?: return

        val factorSourcesById = factorSources.associateBy { (it.id as FactorSourceId.Hash).value }

        signingJob?.cancel()
        signingJob = sign(factorSourcesById)
    }

    fun onMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun resolveFactorSourcesAndSign() {
        val profile = getProfileUseCase()

        val factorSources = input.perFactorSource.map { perFactorSource ->
            profile.factorSourceById(perFactorSource.factorSourceId.asGeneral()) ?: run {
                finishWithFailure(perFactorSource.factorSourceId, NeglectFactorReason.FAILURE)
                return
            }
        }

        val factorSourcesById = factorSources.associateBy { (it.id as FactorSourceId.Hash).value }
        signingJob = sign(factorSourcesById)
    }

    private fun sign(factorSources: Map<FactorSourceIdFromHash, FactorSource>) = viewModelScope.launch {
        if (input.perFactorSource.size > 1) {
            if (input.perFactorSource.any { it.factorSourceId.kind != FactorSourceKind.DEVICE }) {
                error("Poly factor sign is currently only supported for DeviceFactorSource kind.")
            } else {
                _state.update {
                    it.copy(
                        isSigningInProgress = true,
                        factorSourcesToSign = State.FactorSourcesToSign.Poly(
                            kind = input.kind,
                            factorSources = factorSources.values.toList()
                        )
                    )
                }

                signPoly(
                    factorSources = factorSources,
                    perDeviceFactorSource = input.perFactorSource
                )
            }
        } else if (input.perFactorSource.size == 1) {
            val input = input.perFactorSource.first()
            val factorSource = factorSources.getValue(input.factorSourceId)
            _state.update { it.copy(isSigningInProgress = true, factorSourcesToSign = State.FactorSourcesToSign.Mono(factorSource)) }

            signMono(
                factorSource = factorSource,
                input = input
            ).map { listOf(it) }
        } else {
            error("Did not provide any factor source to sign")
        }.onSuccess { signaturesPerFactorSource ->
            _state.update { it.copy(isSigningInProgress = false) }
            finishWithSuccess(signaturesPerFactorSource)
        }.onFailure { error ->
            val errorMessageToShow = if (error is SecureStorageAccessException && error.errorKind == USER_CANCELLED) {
                null
            } else if (error is FailedToSignTransaction && error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction) {
                null
            } else {
                UiMessage.ErrorMessage(error)
            }

            _state.update { it.copy(isSigningInProgress = false, errorMessage = errorMessageToShow) }
        }
    }

    private suspend fun signPoly(
        factorSources: Map<FactorSourceIdFromHash, FactorSource>,
        perDeviceFactorSource: List<InputPerFactorSource<Signable.Payload>>
    ): Result<List<SignaturesPerFactorSource<Signable.ID>>> = signWithDeviceFactorSourceUseCase.poly(
        deviceFactorSources = factorSources.values.filterIsInstance<FactorSource.Device>(),
        inputs = perDeviceFactorSource
    )

    private suspend fun signMono(
        factorSource: FactorSource,
        input: InputPerFactorSource<Signable.Payload>
    ): Result<SignaturesPerFactorSource<Signable.ID>> = when (factorSource) {
        is FactorSource.Device -> signWithDeviceFactorSourceUseCase.mono(
            deviceFactorSource = factorSource,
            input = input
        )

        is FactorSource.Ledger -> signWithLedgerFactorSourceUseCase.mono(
            ledgerFactorSource = factorSource,
            input = input
        )

        else -> error("Signing with ${factorSource.kind} is not yet supported")
    }

    private suspend fun finishWithSuccess(signaturesPerFactorSource: List<SignaturesPerFactorSource<Signable.ID>>) {
        val output = signaturesPerFactorSource.map { OutputPerFactorSource.Signed(signatures = it) }

        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput(output))
    }


    private suspend fun finishWithFailure(
        factorSourceId: FactorSourceIdFromHash,
        neglectFactorReason: NeglectFactorReason
    ) {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput(
                perFactorSource = listOf(
                    OutputPerFactorSource.Neglected(
                        factorSourceId = factorSourceId,
                        reason = neglectFactorReason
                    )
                )
            )
        )
    }

    data class State(
        val signPurpose: AccessFactorSourcesInput.ToSign.Purpose,
        val factorSourcesToSign: FactorSourcesToSign = FactorSourcesToSign.Resolving,
        val isSigningInProgress: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val factorSources: List<FactorSource>? = when (factorSourcesToSign) {
            is FactorSourcesToSign.Mono -> listOf(factorSourcesToSign.factorSource)
            is FactorSourcesToSign.Poly -> factorSourcesToSign.factorSources
            FactorSourcesToSign.Resolving -> null
        }

        sealed interface FactorSourcesToSign {

            data object Resolving : FactorSourcesToSign

            data class Poly(
                val kind: FactorSourceKind,
                val factorSources: List<FactorSource>
            ): FactorSourcesToSign

            data class Mono(
                val factorSource: FactorSource
            ): FactorSourcesToSign
        }
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
