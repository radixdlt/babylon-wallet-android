package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessArculusFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessLedgerHardwareWalletFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessPasswordFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceSkipOption
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.NeglectedFactor
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.Signable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class GetSignaturesViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessDeviceFactorSource: AccessDeviceFactorSourceUseCase,
    private val accessLedgerHardwareWalletFactorSource: AccessLedgerHardwareWalletFactorSourceUseCase,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
    private val accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
    private val accessPasswordFactorSourceUseCase: AccessPasswordFactorSourceUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    getProfileUseCase: GetProfileUseCase
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    private val proxyInput = accessFactorSourcesIOHandler.getInput()
        as AccessFactorSourcesInput.ToSign<out Signable.Payload, out Signable.ID>

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        id = proxyInput.input.factorSourceId.asGeneral(),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback,
        onFailCallback = this::onFailCallback
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

        is FactorSource.ArculusCard -> accessArculusFactorSourceUseCase.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )
        is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )

        is FactorSource.Password -> accessPasswordFactorSourceUseCase.signMono(
            factorSource = factorSource,
            input = proxyInput.input
        )
    }.map { perFactorOutcome ->
        finishWithSuccess(perFactorOutcome)
    }

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (reject)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput.Rejected)
    }

    private suspend fun onFailCallback() {
        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput.Completed(
                outcome = PerFactorOutcome(
                    factorSourceId = proxyInput.input.factorSourceId,
                    outcome = FactorOutcome.Neglected(
                        factor = NeglectedFactor(
                            reason = NeglectFactorReason.FAILURE,
                            factor = proxyInput.input.factorSourceId
                        )
                    )
                )
            )
        )
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
    ) : UiState {

        private val canSkipFactor: Boolean
            get() = signPurpose == AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents ||
                signPurpose == AccessFactorSourcesInput.ToSign.Purpose.SubIntents

        val skipOption: AccessFactorSourceSkipOption
            get() = if (canSkipFactor) AccessFactorSourceSkipOption.CanSkipFactor else AccessFactorSourceSkipOption.None
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
