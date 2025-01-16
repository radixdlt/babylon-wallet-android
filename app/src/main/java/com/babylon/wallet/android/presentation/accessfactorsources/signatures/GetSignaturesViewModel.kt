package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessDeviceFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessLedgerHardwareWalletFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessOffDeviceMnemonicFactorSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.CommonException.SecureStorageAccessException
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.NeglectedFactor
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.OffDeviceMnemonicHint
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.Signable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class GetSignaturesViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessDeviceFactorSource: AccessDeviceFactorSource,
    private val accessLedgerHardwareWalletFactorSource: AccessLedgerHardwareWalletFactorSource,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSource,
    getProfileUseCase: GetProfileUseCase
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    @Suppress("UNCHECKED_CAST")
    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSign<Signable.Payload, Signable.ID>

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        id = proxyInput.input.factorSourceId.asGeneral(),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback
    )

    override fun initialState(): State = State(
        signPurpose = proxyInput.purpose,
        accessState = accessDelegate.state.value,
    )

    init {
        accessDelegate
            .state
            .onEach { accessState ->
                _state.update { it.copy(accessState = accessState) }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun onAccess(factorSource: FactorSource): Result<Unit> = when (factorSource) {
        is FactorSource.Device -> accessDeviceFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )

        is FactorSource.Ledger -> accessLedgerHardwareWalletFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )

        is FactorSource.ArculusCard -> TODO()
        is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )

        is FactorSource.Password -> TODO()
        is FactorSource.SecurityQuestions -> error("Signing with ${factorSource.value.kind} is not supported yet")
        is FactorSource.TrustedContact -> error("Signing with ${factorSource.value.kind} is not supported yet")
    }.map { perFactorOutcome ->
        finishWithSuccess(perFactorOutcome)
    }

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput.Rejected)
    }

    fun onDismiss() = accessDelegate.onDismiss()

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) = accessDelegate.onSeedPhraseWordChanged(wordIndex, word)

    fun onPasswordTyped(password: String) = accessDelegate.onPasswordTyped(password)

    fun onRetry() = accessDelegate.onRetry()

    fun onMessageShown() = accessDelegate.onMessageShown()

    fun onInputConfirmed() = accessDelegate.onInputConfirmed()

    fun onSkip() = viewModelScope.launch {
        accessDelegate.onCancelAccess()

        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        val outcome = FactorOutcome.Neglected<Signable.ID>(
            factor = NeglectedFactor(
                reason = NeglectFactorReason.USER_EXPLICITLY_SKIPPED,
                factor = proxyInput.input.factorSourceId
            )
        )
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput.Completed(
                outcome = PerFactorOutcome(
                    factorSourceId = proxyInput.input.factorSourceId,
                    outcome = outcome
                )
            )
        )
    }

    private suspend fun finishWithSuccess(outcome: PerFactorOutcome<Signable.ID>) {
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput.Completed(outcome = outcome))
    }

    data class State(
        val signPurpose: AccessFactorSourcesInput.ToSign.Purpose,
        val accessState: AccessFactorSourceDelegate.State
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
