package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.seedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.DeviceFactorSourceAddingClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
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
            seedPhraseInputDelegate.setWords(deviceFactorSourceAddingClient.generateMnemonicWords())

            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { state ->
                    state.copy(
                        seedPhraseState = delegateState
                    )
                }
            }
        }
    }

    override fun initialState(): State = State()

    fun onMessageDismiss() {
        _state.update { state -> state.copy(errorMessage = null) }
        viewModelScope.launch { sendEvent(Event.Dismiss) }
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

            deviceFactorSourceAddingClient.isFactorAlreadyInUse()
                .onSuccess { isFactorAlreadyInUse ->
                    if (isFactorAlreadyInUse) {
                        _state.update { state ->
                            state.copy(
                                errorMessage = UiMessage.ErrorMessage(RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse)
                            )
                        }
                    } else {
                        sendEvent(Event.Confirmed)
                    }
                }
                .onFailure {
                    _state.update { state -> state.copy(errorMessage = UiMessage.ErrorMessage(it)) }
                }
        }
    }

    sealed interface Event : OneOffEvent {

        data object Confirmed : Event

        data object Dismiss : Event
    }

    data class State(
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val isEditingEnabled: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isConfirmButtonEnabled = seedPhraseState.isInputComplete()
    }
}