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
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.NeglectedFactor
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSign<Signable.Payload, Signable.ID>

    private var signingJob: Job? = null

    override fun initialState(): State = State(
        signPurpose = proxyInput.purpose
    )

    init {
        signingJob = viewModelScope.launch {
            resolveFactorSourcesAndSign()
        }
    }

    fun onDismiss() = viewModelScope.launch {
        finishWithFailure(
            factorSourceId = proxyInput.input.factorSourceId,
            neglectFactorReason = NeglectFactorReason.USER_EXPLICITLY_SKIPPED
        )
    }

    fun onRetry() {
        val factorSource = _state.value.factorSource ?: return

        signingJob?.cancel()
        signingJob = viewModelScope.launch {
            sign(factorSource)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun resolveFactorSourcesAndSign() {
        val profile = getProfileUseCase()

        val factorSource = profile.factorSourceById(
            id = proxyInput.input.factorSourceId.asGeneral()
        ) ?: run {
            finishWithFailure(proxyInput.input.factorSourceId, NeglectFactorReason.FAILURE)
            return
        }

        sign(factorSource)
    }

    private suspend fun sign(factorSource: FactorSource) {
        _state.update {
            it.copy(isSigningInProgress = true, factorSourcesToSign = State.FactorSourcesToSign.Mono(factorSource))
        }

        signMono(
            factorSource = factorSource,
            input = proxyInput.input
        ).onSuccess { perFactorOutcome ->
            _state.update { it.copy(isSigningInProgress = false) }
            finishWithSuccess(perFactorOutcome)
        }.onFailure { error ->
            val errorMessageToShow = if (error is SecureStorageAccessException && error.errorKind.isManualCancellation()) {
                null
            } else if (error is FailedToSignTransaction && error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction) {
                null
            } else {
                UiMessage.ErrorMessage(error)
            }

            _state.update { it.copy(isSigningInProgress = false, errorMessage = errorMessageToShow) }
        }
    }

    private suspend fun signMono(
        factorSource: FactorSource,
        input: PerFactorSourceInput<Signable.Payload, Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> = when (factorSource) {
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

    private suspend fun finishWithSuccess(outcome: PerFactorOutcome<Signable.ID>) {
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput(output = outcome))
    }

    private suspend fun finishWithFailure(
        factorSourceId: FactorSourceIdFromHash,
        neglectFactorReason: NeglectFactorReason
    ) {
        signingJob?.cancel()

        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)

        val outcome = FactorOutcome.Neglected<Signable.ID>(
            factor = NeglectedFactor(
                reason = neglectFactorReason,
                factor = factorSourceId
            )
        )
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput(
                output = PerFactorOutcome(
                    factorSourceId = factorSourceId,
                    outcome = outcome
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

        val factorSource: FactorSource? = when (factorSourcesToSign) {
            is FactorSourcesToSign.Mono -> factorSourcesToSign.factorSource
            FactorSourcesToSign.Resolving -> null
        }

        sealed interface FactorSourcesToSign {

            data object Resolving : FactorSourcesToSign

            data class Mono(
                val factorSource: FactorSource
            ) : FactorSourcesToSign
        }
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
