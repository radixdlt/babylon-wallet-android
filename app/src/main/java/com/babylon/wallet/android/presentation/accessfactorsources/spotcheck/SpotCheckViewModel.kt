package com.babylon.wallet.android.presentation.accessfactorsources.spotcheck

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
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel.Event
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SpotCheckResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SpotCheckViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessDeviceFactorSource: AccessDeviceFactorSourceUseCase,
    private val accessLedgerHardwareWalletFactorSource: AccessLedgerHardwareWalletFactorSourceUseCase,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
    private val accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
    private val accessPasswordFactorSourceUseCase: AccessPasswordFactorSourceUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    getProfileUseCase: GetProfileUseCase,
) : StateViewModel<SpotCheckViewModel.State>(),
    OneOffEventHandler<SpotCheckViewModel.Event> by OneOffEventHandlerImpl() {

    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSpotCheck

    override fun initialState(): State = State(
        factorSource = proxyInput.factorSource,
        isSkipAllowed = proxyInput.allowSkip,
        accessState = accessDelegate.state.value,
    )

    private val accessDelegate = AccessFactorSourceDelegate(
        viewModelScope = viewModelScope,
        factorSource = proxyInput.factorSource,
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        accessArculusFactorSourceUseCase = accessArculusFactorSourceUseCase,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = this::onAccess,
        onDismissCallback = this::onDismissCallback,
        onFailCallback = {}
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
        is FactorSource.Device -> accessDeviceFactorSource.spotCheck(factorSource = factorSource)
        is FactorSource.Ledger -> accessLedgerHardwareWalletFactorSource.spotCheck(factorSource = factorSource)
        is FactorSource.ArculusCard -> accessArculusFactorSourceUseCase.spotCheck(factorSource = factorSource)
        is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.spotCheck(factorSource = factorSource)
        is FactorSource.Password -> accessPasswordFactorSourceUseCase.spotCheck(factorSource = factorSource)
    }.map { isValidated ->
        if (isValidated) {
            sendEvent(event = Event.Completed)
            accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SpotCheckOutput.Completed(response = SpotCheckResponse.VALID))
        }
    }

    private suspend fun onDismissCallback() {
        // end the signing process and return the output (reject)
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SpotCheckOutput.Rejected)
    }

    fun onDismiss() = accessDelegate.onDismiss()

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) = accessDelegate.onSeedPhraseWordChanged(wordIndex, word)

    fun onPasswordTyped(password: String) = accessDelegate.onPasswordTyped(password)

    fun onArculusPinChange(pin: String) = accessDelegate.onArculusPinChange(pin)

    fun onForgotArculusPinClick() = accessDelegate.onForgotArculusPinClick()

    fun onArculusInfoMessageDismiss() = accessDelegate.onArculusInfoMessageDismiss()

    fun onRetry() = accessDelegate.onRetry()

    fun onMessageShown() = accessDelegate.onMessageShown()

    fun onInputConfirmed() = accessDelegate.onInputConfirmed()

    fun onIgnore() = viewModelScope.launch {
        accessDelegate.onCancelAccess()

        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SpotCheckOutput.Completed(SpotCheckResponse.SKIPPED))
    }

    data class State(
        val factorSource: FactorSource,
        private val isSkipAllowed: Boolean,
        val accessState: AccessFactorSourceDelegate.State
    ) : UiState {

        val skipOption: AccessFactorSourceSkipOption
            get() = if (isSkipAllowed) {
                AccessFactorSourceSkipOption.CanIgnoreFactor
            } else {
                AccessFactorSourceSkipOption.None
            }
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
