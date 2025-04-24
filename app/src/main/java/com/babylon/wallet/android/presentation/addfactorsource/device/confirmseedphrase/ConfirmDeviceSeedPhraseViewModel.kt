package com.babylon.wallet.android.presentation.addfactorsource.device.confirmseedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.DeviceMnemonicBuilderClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.DeviceMnemonicValidationOutcome
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import javax.inject.Inject

@HiltViewModel
class ConfirmDeviceSeedPhraseViewModel @Inject constructor(
    private val deviceMnemonicBuilderClient: DeviceMnemonicBuilderClient
) : StateViewModel<ConfirmDeviceSeedPhraseViewModel.State>(),
    OneOffEventHandler<ConfirmDeviceSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    words = deviceMnemonicBuilderClient.generateConfirmationWords().toPersistentList()
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
            when (val outcome = deviceMnemonicBuilderClient.confirmWords(state.value.words)) {
                DeviceMnemonicValidationOutcome.Valid -> {
                    sendEvent(Event.Confirmed(deviceMnemonicBuilderClient.getMnemonicWithPassphrase()))
                }
                is DeviceMnemonicValidationOutcome.Invalid -> {
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
                    words = deviceMnemonicBuilderClient.getWords(SeedPhraseWord.State.NotEmpty)
                        .filter { it.index in indicesToConfirm }
                        .toPersistentList()
                )
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data class Confirmed(
            val mnemonicWithPassphrase: MnemonicWithPassphrase
        ) : Event
    }

    data class State(
        val factorSourceKind: FactorSourceKind = FactorSourceKind.DEVICE,
        val words: ImmutableList<SeedPhraseWord> = persistentListOf(),
    ) : UiState {

        val isConfirmButtonEnabled = words.all { it.hasValue }
    }
}
