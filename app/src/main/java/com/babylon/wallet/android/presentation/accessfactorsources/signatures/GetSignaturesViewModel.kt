package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.babylon.wallet.android.domain.usecases.transaction.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.State.FactorSourceRequest.DeviceRequest
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.State.FactorSourceRequest.LedgerRequest
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.kind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val input = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToGetSignatures

    private var collectSignaturesWithDeviceJob: Job? = null
    private var collectSignaturesWithLedgerJob: Job? = null

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val signersPerFactorSource = getSigningEntitiesByFactorSource(signers = input.signers)

            val requests = mutableListOf<State.FactorSourceRequest>()
            signersPerFactorSource.forEach { (factorSource, entities) ->
                when (factorSource) {
                    is FactorSource.Device -> {
                        val deviceRequest = (
                            requests
                                .find { request ->
                                    request is DeviceRequest
                                } as? DeviceRequest
                            ) ?: DeviceRequest(mutableMapOf()).also {
                            requests.add(it)
                        }

                        deviceRequest.deviceFactorSources[factorSource] = entities
                    }

                    is FactorSource.Ledger -> {
                        requests.add(
                            LedgerRequest(
                                factorSource = factorSource,
                                entities = entities
                            )
                        )
                    }
                }
            }

            _state.update { state -> state.copy(signersRequests = requests) }
            proceedToNextSigners()
        }
    }

    // Currently if one of the signatures fails it means ending a whole signing process,
    // but with MFA, this may not be the case, we may be able to proceed to try to get other signatures.
    private suspend fun proceedToNextSigners() {
        _state.update { it.proceedToNextSigners() }

        when (val request = state.value.nextRequest) {
            is LedgerRequest -> {
                _state.update { state ->
                    state.copy(
                        showContentForFactorSource = State.ShowContentForFactorSource.Ledger(ledgerFactorSource = request.factorSource)
                    )
                }
                collectSignaturesForLedgerFactorSource(
                    ledgerFactorSource = request.factorSource,
                    signers = request.entities,
                    signRequest = input.signRequest
                )
            }

            is DeviceRequest -> {
                _state.update { state ->
                    state.copy(showContentForFactorSource = State.ShowContentForFactorSource.Device)
                }
                sendEvent(event = Event.RequestBiometricToAccessDeviceFactorSources)
            }

            null -> {
                accessFactorSourcesIOHandler.setOutput(
                    output = AccessFactorSourcesOutput.Signatures(
                        signaturesWithPublicKey = state.value.signaturesWithPublicKeys
                    )
                )
                sendEvent(event = Event.AccessingFactorSourceCompleted)
            }
        }
    }

    fun onRetryClick() {
        viewModelScope.launch {
            val signersPerFactorSource = getSigningEntitiesByFactorSource(input.signers)

            when (val content = state.value.showContentForFactorSource) {
                is State.ShowContentForFactorSource.Device -> {
                    sendEvent(event = Event.RequestBiometricToAccessDeviceFactorSources)
                }

                is State.ShowContentForFactorSource.Ledger -> {
                    val signersWithLedgerFactor = signersPerFactorSource[content.ledgerFactorSource] ?: return@launch
                    collectSignaturesForLedgerFactorSource(
                        ledgerFactorSource = content.ledgerFactorSource,
                        signers = signersWithLedgerFactor,
                        signRequest = input.signRequest

                    )
                }

                State.ShowContentForFactorSource.None -> {}
            }
        }
    }

    // user clicked on X icon or navigated back thus the bottom sheet is dismissed
    fun onUserDismiss() {
        viewModelScope.launch {
            accessFactorSourcesIOHandler.setOutput(
                output = AccessFactorSourcesOutput.Failure(RadixWalletException.DappRequestException.RejectedByUser)
            )
            sendEvent(Event.UserDismissed)
        }
    }

    fun collectSignaturesForDeviceFactorSource() {
        collectSignaturesWithDeviceJob?.cancel()
        collectSignaturesWithDeviceJob = viewModelScope.launch {
            val request = state.value.nextRequest as? DeviceRequest ?: return@launch
            val signatures = request.deviceFactorSources.flatMap { (deviceFactorSource, entities) ->
                signWithDeviceFactorSourceUseCase(
                    deviceFactorSource = deviceFactorSource,
                    signers = entities,
                    signRequest = input.signRequest
                ).onFailure {
                    // return the output (error) and end the signing process
                    accessFactorSourcesIOHandler.setOutput(
                        AccessFactorSourcesOutput.Failure(error = it)
                    )
                    sendEvent(event = Event.AccessingFactorSourceCompleted)
                }.getOrNull() ?: return@launch
            }

            _state.update { state -> state.addSignatures(signatures) }
            proceedToNextSigners()
        }
    }

    private fun collectSignaturesForLedgerFactorSource(
        ledgerFactorSource: FactorSource.Ledger,
        signers: List<ProfileEntity>,
        signRequest: SignRequest
    ) {
        collectSignaturesWithLedgerJob?.cancel()
        collectSignaturesWithLedgerJob = viewModelScope.launch {
            signWithLedgerFactorSourceUseCase(
                ledgerFactorSource = ledgerFactorSource,
                signers = signers,
                signRequest = signRequest
            ).onSuccess { signatures ->
                _state.update { state -> state.addSignatures(signatures) }
                proceedToNextSigners()
            }.onFailure { error ->
                if (error is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction &&
                    error.reason == LedgerErrorCode.Generic // Generic can mean anything, e.g. user closed the browser tab
                ) {
                    return@launch // should be return@launch to keep the bottom sheet open
                }

                // otherwise return the output (error)
                accessFactorSourcesIOHandler.setOutput(
                    output = AccessFactorSourcesOutput.Failure(error = error)
                )
                // and end the signing process
                sendEvent(event = Event.AccessingFactorSourceCompleted)
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private suspend fun getSigningEntitiesByFactorSource(
        signers: List<ProfileEntity>
    ): Map<FactorSource, List<ProfileEntity>> = withContext(defaultDispatcher) {
        val result = mutableMapOf<FactorSource, List<ProfileEntity>>()
        val profile = getProfileUseCase()

        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is EntitySecurityState.Unsecured -> {
                    val factorSourceId = securityState.value.transactionSigning.factorSourceId.asGeneral()
                    val factorSource = requireNotNull(profile.factorSourceById(factorSourceId))

                    if (factorSource.kind != FactorSourceKind.TRUSTED_CONTACT) { // trusted contact cannot sign!
                        if (result[factorSource] != null) {
                            result[factorSource] = result[factorSource].orEmpty() + listOf(signer)
                        } else {
                            result[factorSource] = listOf(signer)
                        }
                    }
                }
            }
        }

        result.keys
            .sortedBy { factorSource ->
                factorSource.kind.signingOrder()
            }.associateWith { factorSource ->
                result[factorSource]!!
            }
    }

    private fun FactorSourceKind.signingOrder(): Int { // based on difficulty - most "challenging" factor comes first
        return when (this) {
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> 0
            FactorSourceKind.DEVICE -> 1
            else -> Int.MAX_VALUE // it doesn't matter because we add only the ledger or device factor sources
        }
    }

    data class State(
        private val signersRequests: List<FactorSourceRequest> = emptyList(),
        private val selectedSignersIndex: Int = -1,
        // list to keep signatures from all factor sources. This will be returned as output once all signers are done.
        val signaturesWithPublicKeys: List<SignatureWithPublicKey> = emptyList(),
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.None,
    ) : UiState {

        sealed class FactorSourceRequest {

            data class LedgerRequest(
                val factorSource: FactorSource.Ledger,
                val entities: List<ProfileEntity>
            ) : FactorSourceRequest()

            data class DeviceRequest(
                val deviceFactorSources: MutableMap<FactorSource.Device, List<ProfileEntity>>
            ) : FactorSourceRequest()
        }

        val nextRequest: FactorSourceRequest?
            get() = signersRequests.getOrNull(selectedSignersIndex)

        fun proceedToNextSigners() = copy(selectedSignersIndex = selectedSignersIndex + 1)

        fun addSignatures(signatures: List<SignatureWithPublicKey>) = copy(
            signaturesWithPublicKeys = signaturesWithPublicKeys.toMutableList().apply { addAll(signatures) }
        )

        sealed interface ShowContentForFactorSource {

            data object None : ShowContentForFactorSource

            data object Device : ShowContentForFactorSource

            data class Ledger(val ledgerFactorSource: FactorSource.Ledger) : ShowContentForFactorSource
        }
    }

    sealed interface Event : OneOffEvent {

        data object RequestBiometricToAccessDeviceFactorSources : Event

        data object AccessingFactorSourceCompleted : Event

        data object UserDismissed : Event
    }
}