package com.babylon.wallet.android.presentation.addfactorsource.seedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.MnemonicBuilderClient
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.name
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.olympia
import javax.inject.Inject

@HiltViewModel
class SeedPhraseViewModel @Inject constructor(
    addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val mnemonicBuilderClient: MnemonicBuilderClient,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<SeedPhraseViewModel.State>(),
    OneOffEventHandler<SeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    init {
        viewModelScope.launch {
            when (input.context) {
                AddFactorSourceInput.Context.New -> {
                    seedPhraseInputDelegate.setWords(mnemonicBuilderClient.generateMnemonicWords())
                }

                is AddFactorSourceInput.Context.Recovery -> {
                    seedPhraseInputDelegate.setSeedPhraseSize(Bip39WordCount.TWENTY_FOUR)
                    _state.update { state -> state.copy(isEditingEnabled = true) }
                }
            }
        }

        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { state ->
                    state.copy(
                        seedPhraseState = delegateState
                    )
                }
            }
        }
    }

    override fun initialState(): State = State(context = input.context)

    fun onDismissMessage() {
        viewModelScope.launch {
            sendEvent(
                if (state.value.errorMessage?.error is RadixWalletException.FactorSource.FactorSourceAlreadyInUse) {
                    Event.DismissFlow
                } else {
                    Event.Dismiss
                }
            )
        }
        _state.update { state -> state.copy(errorMessage = null) }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
    }

    fun onEnterCustomSeedPhraseClick() {
        _state.update { state -> state.copy(isEditingEnabled = true) }

        seedPhraseInputDelegate.setWords(
            _state.value.seedPhraseState.seedPhraseWords.map { word ->
                word.copy(
                    value = "",
                    state = SeedPhraseWord.State.Empty
                )
            }
        )
    }

    fun onNumberOfWordsChanged(wordCount: Bip39WordCount) {
        seedPhraseInputDelegate.setSeedPhraseSize(wordCount)
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            if (state.value.isEditingEnabled) {
                _state.update { state ->
                    state.copy(
                        seedPhraseState = state.seedPhraseState.copy(
                            seedPhraseWords = mnemonicBuilderClient.createMnemonicFromWords(state.seedPhraseState.seedPhraseWords)
                                .toPersistentList()
                        )
                    )
                }
            }

            mnemonicBuilderClient.isFactorAlreadyInUse(input.kind)
                .then { isFactorAlreadyInUse ->
                    val existingFactorSource = mnemonicBuilderClient.getExistingFactorSource(input.kind)
                        .getOrNull()

                    appendCryptoParametersIfNeeded(isFactorAlreadyInUse, existingFactorSource)
                        .map { existingFactorSource }
                }
                .onSuccess { existingFactorSource ->
                    if (existingFactorSource == null) {
                        sendEvent(Event.Confirmed)
                    } else {
                        _state.update { state ->
                            state.copy(
                                errorMessage = UiMessage.ErrorMessage(
                                    error = RadixWalletException.FactorSource.FactorSourceAlreadyInUse(
                                        factorSourceName = existingFactorSource.name
                                    )
                                )
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { state -> state.copy(errorMessage = UiMessage.ErrorMessage(it)) }
                }
        }
    }

    private suspend fun appendCryptoParametersIfNeeded(
        isFactorAlreadyInUse: Boolean,
        factorSource: FactorSource?
    ): Result<Unit> = when {
        !isFactorAlreadyInUse || input.kind != FactorSourceKind.DEVICE -> {
            Result.success(Unit)
        }

        factorSource == null -> {
            error("If the factor source is in use it should have been found in profile")
        }

        else -> {
            sargonOsManager.callSafely(dispatcher) {
                appendCryptoParametersToFactorSource(
                    factorSourceId = factorSource.id,
                    cryptoParameters = when (input.context) {
                        AddFactorSourceInput.Context.New -> FactorSourceCryptoParameters.babylon
                        is AddFactorSourceInput.Context.Recovery -> if (input.context.isOlympia) {
                            FactorSourceCryptoParameters.olympia
                        } else {
                            FactorSourceCryptoParameters.babylon
                        }
                    }
                )
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data object Confirmed : Event

        data object DismissFlow : Event

        data object Dismiss : Event
    }

    data class State(
        val context: AddFactorSourceInput.Context,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val isEditingEnabled: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isOlympiaRecovery = context is AddFactorSourceInput.Context.Recovery && context.isOlympia
        val isConfirmButtonEnabled = seedPhraseState.isInputComplete()
    }
}
