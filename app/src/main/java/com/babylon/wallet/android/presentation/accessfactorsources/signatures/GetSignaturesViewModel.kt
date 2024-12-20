
package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    private var collectSignaturesWithDeviceJob: Job? = null
    private var collectSignaturesWithLedgerJob: Job? = null

    private val input = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSign<Signable.Payload>

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            resolveFactorSourcesAndSign()
        }
    }

    private suspend fun resolveFactorSourcesAndSign() {
        val profile = getProfileUseCase()

        val factorSources = input.perFactorSource.map { perFactorSource ->
            profile.factorSourceById(perFactorSource.factorSourceId.asGeneral()) ?: run {
                finishWithFailure(perFactorSource.factorSourceId, NeglectFactorReason.FAILURE)
                return
            }
        }

        _state.update { it.copy(factorSourcesToSign = State.FactorSourcesToSign.Resolved(factorSources)) }

        sign(factorSources.associateBy { (it.id as FactorSourceId.Hash).value })
    }

    private suspend fun sign(factorSources: Map<FactorSourceIdFromHash, FactorSource>) {
        if (input.perFactorSource.size > 1) {
            if (input.perFactorSource.any { it.factorSourceId.kind != FactorSourceKind.DEVICE }) {
                error("Poly factor sign is currently only supported for DeviceFactorSource kind.")
            } else {
                signPoly(
                    factorSources = factorSources,
                    perDeviceFactorSource = input.perFactorSource
                )
            }
        } else if (input.perFactorSource.size == 1) {
            val input = input.perFactorSource.first()
            signMono(
                factorSource = factorSources.getValue(input.factorSourceId),
                input = input
            )
        } else {
            error("Did not provide any factor source to sign")
        }
    }

    private suspend fun signPoly(
        factorSources: Map<FactorSourceIdFromHash, FactorSource>,
        perDeviceFactorSource: List<InputPerFactorSource<Signable.Payload>>
    ): Result<List<HdSignature<Signable.ID>>> {
        return signWithDeviceFactorSourceUseCase.poly(
            deviceFactorSources = factorSources.values.filterIsInstance<FactorSource.Device>(),
            inputs = perDeviceFactorSource
        )
    }

    private suspend fun signMono(
        factorSource: FactorSource,
        input: InputPerFactorSource<Signable.Payload>
    ): Result<List<HdSignature<Signable.ID>>> {
        return when (factorSource) {
            is FactorSource.Device -> signWithDeviceFactorSourceUseCase.mono(
                deviceFactorSource = factorSource,
                input = input
            )
            is FactorSource.Ledger -> TODO()
            else -> error("Signing with ${factorSource.kind} is not yet supported")
        }
    }



    private suspend fun finishWithFailure(
        factorSourceId: FactorSourceIdFromHash,
        neglectFactorReason: NeglectFactorReason
    ) {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput(
                perFactorSource = listOf(OutputPerFactorSource.Neglected(
                    factorSourceId = factorSourceId,
                    reason = neglectFactorReason
                ))
            )
        )
    }

    data class State(
        val factorSourcesToSign: FactorSourcesToSign = FactorSourcesToSign.Resolving
    ) : UiState {

        sealed interface FactorSourcesToSign {
            data object Resolving: FactorSourcesToSign

            data class Resolved(
                val factorSources: List<FactorSource>
            ): FactorSourcesToSign
        }

    }

    sealed interface Event : OneOffEvent {
        data object Completed: Event
    }
}
