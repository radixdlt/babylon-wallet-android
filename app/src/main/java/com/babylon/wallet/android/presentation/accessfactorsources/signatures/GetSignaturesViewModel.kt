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
import com.radixdlt.sargon.FactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.FactorOutcomeOfSubintentHash
import com.radixdlt.sargon.FactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.NeglectedFactor
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.extensions.asGeneral
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

    enum class Purpose {
        TransactionIntents,
        SubIntents,
        AuthIntents
    }

    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.Sign
    private val purpose: Purpose = when (proxyInput) {
        is AccessFactorSourcesInput.SignTransaction -> Purpose.TransactionIntents
        is AccessFactorSourcesInput.SignSubintent -> Purpose.SubIntents
        is AccessFactorSourcesInput.SignAuth -> Purpose.AuthIntents
    }

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        id = proxyInput.factorSourceId.asGeneral(),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        accessArculusFactorSourceUseCase = accessArculusFactorSourceUseCase,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback,
        onFailCallback = this::onFailCallback
    )

    override fun initialState(): State = State(
        signPurpose = purpose,
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
            input = proxyInput
        )

        is FactorSource.Ledger -> accessLedgerHardwareWalletFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput
        )

        is FactorSource.ArculusCard -> accessArculusFactorSourceUseCase.signMono(
            factorSource = factorSource,
            input = proxyInput
        )
        is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.signMono(
            factorSource = factorSource,
            input = proxyInput
        )

        is FactorSource.Password -> accessPasswordFactorSourceUseCase.signMono(
            factorSource = factorSource,
            input = proxyInput
        )
    }.map { perFactorOutcome ->
        finishWithSuccess(perFactorOutcome)
    }

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (reject)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignRejected)
    }

    private suspend fun onFailCallback() {
        // end the signing process and return the output (error)
        completeWithNeglectedFactorOutput(
            NeglectedFactor(
                reason = NeglectFactorReason.FAILURE,
                factor = proxyInput.factorSourceId
            )
        )
    }

    private suspend fun completeWithNeglectedFactorOutput(neglectedFactor: NeglectedFactor) {
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(
            when (proxyInput) {
                is AccessFactorSourcesInput.SignTransaction -> AccessFactorSourcesOutput.SignTransaction(
                    PerFactorOutcomeOfTransactionIntentHash(
                        factorSourceId = proxyInput.factorSourceId,
                        outcome = FactorOutcomeOfTransactionIntentHash.Neglected(
                            neglectedFactor
                        )
                    )
                )
                is AccessFactorSourcesInput.SignSubintent -> AccessFactorSourcesOutput.SignSubintent(
                    PerFactorOutcomeOfSubintentHash(
                        factorSourceId = proxyInput.factorSourceId,
                        outcome = FactorOutcomeOfSubintentHash.Neglected(
                            neglectedFactor
                        )
                    )
                )
                is AccessFactorSourcesInput.SignAuth -> AccessFactorSourcesOutput.SignAuth(
                    PerFactorOutcomeOfAuthIntentHash(
                        factorSourceId = proxyInput.factorSourceId,
                        outcome = FactorOutcomeOfAuthIntentHash.Neglected(
                            neglectedFactor
                        )
                    )
                )
            }
        )
    }

    fun onDismiss() = accessDelegate.onDismiss()

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) = accessDelegate.onSeedPhraseWordChanged(wordIndex, word)

    fun onPasswordTyped(password: String) = accessDelegate.onPasswordTyped(password)

    fun onArculusPinChange(pin: String) = accessDelegate.onArculusPinChange(pin)

    fun onRetry() = accessDelegate.onRetry()

    fun onMessageShown() = accessDelegate.onMessageShown()

    fun onInputConfirmed() = accessDelegate.onInputConfirmed()

    fun onSkip() = viewModelScope.launch {
        accessDelegate.onCancelAccess()

        // end the signing process and return the output (error)
        completeWithNeglectedFactorOutput(
            NeglectedFactor(
                reason = NeglectFactorReason.USER_EXPLICITLY_SKIPPED,
                factor = proxyInput.factorSourceId
            )
        )
    }

    private suspend fun finishWithSuccess(outcome: AccessFactorSourcesOutput.Sign) {
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(outcome)
    }

    data class State(
        val signPurpose: Purpose,
        val accessState: AccessFactorSourceDelegate.State
    ) : UiState {

        private val canSkipFactor: Boolean = false
//            get() = signPurpose == AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents ||
//                signPurpose == AccessFactorSourcesInput.ToSign.Purpose.SubIntents

        val skipOption: AccessFactorSourceSkipOption
            get() = if (canSkipFactor) AccessFactorSourceSkipOption.CanSkipFactor else AccessFactorSourceSkipOption.None
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
