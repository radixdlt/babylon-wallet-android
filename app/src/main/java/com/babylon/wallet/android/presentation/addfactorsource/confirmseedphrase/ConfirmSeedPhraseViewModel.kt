package com.babylon.wallet.android.presentation.addfactorsource.confirmseedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.MnemonicBuilderClient
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIntermediaryParams
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicValidationOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import javax.inject.Inject

@HiltViewModel
class ConfirmSeedPhraseViewModel @Inject constructor(
    private val mnemonicBuilderClient: MnemonicBuilderClient,
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler
) : StateViewModel<ConfirmSeedPhraseViewModel.State>(),
    OneOffEventHandler<ConfirmSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind

    init {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    words = mnemonicBuilderClient.generateConfirmationWords().toPersistentList()
                )
            }
        }
    }

    override fun initialState(): State = State()

    fun onWordChanged(index: Int, value: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    words = state.words.mapWhen(
                        predicate = { it.index == index },
                        mutation = {
                            val newValue = value.trim()
                            it.copy(
                                value = newValue,
                                state = when {
                                    newValue.isBlank() -> SeedPhraseWord.State.Empty
                                    else -> SeedPhraseWord.State.NotEmpty
                                }
                            )
                        }
                    ).toPersistentList()
                )
            }
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            when (val outcome = mnemonicBuilderClient.confirmWords(state.value.words)) {
                MnemonicValidationOutcome.Valid -> {
                    when (input.kind) {
                        FactorSourceKind.DEVICE -> {
                            addFactorSourceIOHandler.setIntermediaryParams(
                                AddFactorSourceIntermediaryParams.Device(
                                    mnemonicWithPassphrase = mnemonicBuilderClient.getMnemonicWithPassphrase()
                                )
                            )
                            sendEvent(Event.DeviceSeedPhraseConfirmed)
                        }

                        FactorSourceKind.ARCULUS_CARD -> {
                            addFactorSourceIOHandler.setIntermediaryParams(
                                AddFactorSourceIntermediaryParams.Arculus(
                                    mnemonicWithPassphrase = mnemonicBuilderClient.getMnemonicWithPassphrase(),
                                    pin = ""
                                )
                            )
                            sendEvent(Event.ArculusSeedPhraseConfirmed)
                        }

                        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        FactorSourceKind.OFF_DEVICE_MNEMONIC,
                        FactorSourceKind.PASSWORD -> error("")
                    }
                }

                is MnemonicValidationOutcome.Invalid -> {
                    val incorrectIndices = outcome.indicesInMnemonic.map { it.toInt() }

                    _state.update { state ->
                        state.copy(
                            words = state.words.mapWhen(
                                predicate = { it.index in incorrectIndices },
                                mutation = { it.copy(state = SeedPhraseWord.State.Invalid) }
                            ).toPersistentList()
                        )
                    }
                }
            }
        }
    }

    fun onDebugFillWordsClick() {
        viewModelScope.launch {
            _state.update { state ->
                val indicesToConfirm = state.words.map { it.index }
                state.copy(
                    words = mnemonicBuilderClient.getWords(SeedPhraseWord.State.NotEmpty)
                        .filter { it.index in indicesToConfirm }
                        .toPersistentList()
                )
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data object DeviceSeedPhraseConfirmed : Event

        data object ArculusSeedPhraseConfirmed : Event
    }

    data class State(
        val factorSourceKind: FactorSourceKind = FactorSourceKind.DEVICE,
        val words: ImmutableList<SeedPhraseWord> = persistentListOf(),
    ) : UiState {

        val isConfirmButtonEnabled = words.all { it.hasValue }
    }
}
