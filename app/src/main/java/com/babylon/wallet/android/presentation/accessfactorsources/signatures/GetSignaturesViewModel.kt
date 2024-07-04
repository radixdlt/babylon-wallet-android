package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.babylon.wallet.android.domain.usecases.transaction.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class GetSignaturesViewModel @Inject constructor(
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy,
    private val signWithDeviceFactorSourceUseCase: SignWithDeviceFactorSourceUseCase,
    private val signWithLedgerFactorSourceUseCase: SignWithLedgerFactorSourceUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    // list to keep signatures from all factor sources. This will be returned as output once all signers are done.
    private val signaturesWithPublicKeys = mutableListOf<SignatureWithPublicKey>()

    private val isSigningWithDeviceInProgress = MutableStateFlow(false)
    private val isSigningWithLedgerInProgress = MutableStateFlow(false)

    private var collectSignaturesWithDeviceJob: Job? = null
    private var collectSignaturesWithLedgerJob: Job? = null

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToGetSignatures
            val signersPerFactorSource = getSigningEntitiesByFactorSource(input.signers)

            signersPerFactorSource.forEach { (factorSource, signers) ->
                when (factorSource) {
                    is FactorSource.Device -> {
                        _state.update { state ->
                            state.copy(
                                showContentForFactorSource = State.ShowContentForFactorSource.Device(deviceFactorSource = factorSource)
                            )
                        }
                        isSigningWithDeviceInProgress.emit(true)
                        sendEvent(
                            event = Event.RequestBiometricToAccessDeviceFactorSource(
                                deviceFactorSource = factorSource,
                                signers = signers,
                                signRequest = input.signRequest
                            )
                        )
                        // wait until the signing with device is complete
                        isSigningWithDeviceInProgress.takeWhile { isInProgress -> isInProgress }.collect()
                    }

                    is FactorSource.Ledger -> {
                        _state.update { state ->
                            state.copy(
                                showContentForFactorSource = State.ShowContentForFactorSource.Ledger(ledgerFactorSource = factorSource)
                            )
                        }
                        isSigningWithLedgerInProgress.emit(true)
                        collectSignaturesForLedgerFactorSource(
                            ledgerFactorSource = factorSource,
                            signers = signers,
                            signRequest = input.signRequest
                        )
                        // wait until the signing with ledger is complete
                        isSigningWithLedgerInProgress.takeWhile { isInProgress -> isInProgress }.collect()
                    }
                }
            }

            accessFactorSourcesUiProxy.setOutput(output = AccessFactorSourcesOutput.Signatures(signaturesWithPublicKeys))
            sendEvent(event = Event.AccessingFactorSourceCompleted)
        }
    }

    fun onRetryClick() {
        viewModelScope.launch {
            val input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.ToGetSignatures
            val signersPerFactorSource = getSigningEntitiesByFactorSource(input.signers)

            when (val content = state.value.showContentForFactorSource) {
                is State.ShowContentForFactorSource.Device -> {
                    val signersWithDeviceFactorSource =
                        signersPerFactorSource[content.deviceFactorSource] ?: return@launch
                    sendEvent(
                        event = Event.RequestBiometricToAccessDeviceFactorSource(
                            deviceFactorSource = content.deviceFactorSource,
                            signers = signersWithDeviceFactorSource,
                            signRequest = input.signRequest
                        )
                    )
                }

                is State.ShowContentForFactorSource.Ledger -> {
                    val signersWithLedgerFactorSource =
                        signersPerFactorSource[content.ledgerFactorSource] ?: return@launch
                    collectSignaturesForLedgerFactorSource(
                        ledgerFactorSource = content.ledgerFactorSource,
                        signers = signersWithLedgerFactorSource,
                        signRequest = input.signRequest

                    )
                }

                State.ShowContentForFactorSource.None -> {}
            }
        }
    }

    fun onUserDismiss() {
        viewModelScope.launch {
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.Failure(RadixWalletException.DappRequestException.RejectedByUser)
            )
            sendEvent(Event.UserDismissed)
        }
    }

    fun collectSignaturesForDeviceFactorSource(
        deviceFactorSource: FactorSource.Device,
        signers: List<ProfileEntity>,
        signRequest: SignRequest
    ) {
        collectSignaturesWithDeviceJob?.cancel()
        collectSignaturesWithDeviceJob = viewModelScope.launch {
            signWithDeviceFactorSourceUseCase(
                deviceFactorSource = deviceFactorSource,
                signers = signers,
                signRequest = signRequest
            ).onSuccess { signatures ->
                signaturesWithPublicKeys.addAll(signatures)
            }.onFailure {
                accessFactorSourcesUiProxy.setOutput(
                    AccessFactorSourcesOutput.Failure(error = it)
                )
            }.also {
                isSigningWithDeviceInProgress.emit(false)
            }
        }
    }

    private suspend fun collectSignaturesForLedgerFactorSource(
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
                signaturesWithPublicKeys.addAll(signatures)
            }.onFailure {
                accessFactorSourcesUiProxy.setOutput(
                    AccessFactorSourcesOutput.Failure(error = it)
                )
            }.also {
                isSigningWithLedgerInProgress.emit(false)
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

    private fun FactorSourceKind.signingOrder(): Int {
        return when (this) {
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> 1
            FactorSourceKind.DEVICE -> 0 // DEVICE should always go first since we authorize KeyStore encryption key for 30s
            else -> Int.MAX_VALUE // it doesn't matter because we add only the ledger or device factor sources
        }
    }

    data class State(
        val showContentForFactorSource: ShowContentForFactorSource = ShowContentForFactorSource.None
    ) : UiState {

        sealed interface ShowContentForFactorSource {

            data object None : ShowContentForFactorSource

            data class Device(val deviceFactorSource: FactorSource.Device) : ShowContentForFactorSource

            data class Ledger(val ledgerFactorSource: FactorSource.Ledger) : ShowContentForFactorSource
        }
    }

    sealed interface Event : OneOffEvent {

        data class RequestBiometricToAccessDeviceFactorSource(
            val deviceFactorSource: FactorSource.Device,
            val signers: List<ProfileEntity>,
            val signRequest: SignRequest
        ) : Event

        data object AccessingFactorSourceCompleted : Event

        data object UserDismissed : Event
    }
}
