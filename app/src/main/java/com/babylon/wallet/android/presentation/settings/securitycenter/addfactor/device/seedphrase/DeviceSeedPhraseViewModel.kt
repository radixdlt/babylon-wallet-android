package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.seedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.DeviceFactorSourceAddingClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceSeedPhraseViewModel @Inject constructor(
    private val deviceFactorSourceAddingClient: DeviceFactorSourceAddingClient
) : StateViewModel<DeviceSeedPhraseViewModel.State>(),
    OneOffEventHandler<DeviceSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    init {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    seedPhraseState = state.seedPhraseState.copy(
                        seedPhraseWords = deviceFactorSourceAddingClient.generateMnemonicWords().toPersistentList()
                    )
                )
            }
        }
    }

    override fun initialState(): State = State()

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
    }

    fun onEnterCustomSeedPhraseClick() {
        _state.update { state ->
            state.copy(
                seedPhraseState = state.seedPhraseState.copy(
                    seedPhraseWords = state.seedPhraseState.seedPhraseWords.map { word ->
                        word.copy(
                            value = "",
                            state = SeedPhraseWord.State.Empty
                        )
                    }.toPersistentList()
                )
            )
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            if (state.value.isEditingEnabled) {
                _state.update { state ->
                    state.copy(
                        seedPhraseState = state.seedPhraseState.copy(
                            seedPhraseWords = deviceFactorSourceAddingClient.createMnemonicFromWords(state.seedPhraseState.seedPhraseWords)
                                .toPersistentList()
                        ),
                    )
                }
            }

            if (state.value.seedPhraseState.isInputComplete()) {
                sendEvent(Event.Confirmed)
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data object Confirmed : Event
    }

    data class State(
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val isEditingEnabled: Boolean = false,
    ) : UiState
}