package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.forgotpin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.MnemonicBuilderClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
class ForgotArculusPinViewModel @Inject constructor(
    private val mnemonicBuilderClient: MnemonicBuilderClient,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ForgotArculusPinViewModel.State>(),
    OneOffEventHandler<ForgotArculusPinViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ForgotArculusPinArgs(savedStateHandle)

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    init {
        viewModelScope.launch {
            seedPhraseInputDelegate.setSeedPhraseSize(Bip39WordCount.TWENTY_FOUR)
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

    override fun initialState(): State = State()

    fun onDismissMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
    }

    fun onNumberOfWordsChanged(wordCount: Bip39WordCount) {
        seedPhraseInputDelegate.setSeedPhraseSize(wordCount)
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            mnemonicBuilderClient.createMnemonicFromWords(state.value.seedPhraseState.seedPhraseWords)

            if (args.factorSourceId != mnemonicBuilderClient.getFactorSourceId(FactorSourceKind.ARCULUS_CARD)) {
                _state.update { state ->
                    state.copy(
                        errorMessage = UiMessage.ErrorMessage(ProfileException.InvalidMnemonic)
                    )
                }

                return@launch
            }

            sendEvent(Event.Complete)
        }
    }

    sealed interface Event : OneOffEvent {

        data object Complete : Event

        data object Dismiss : Event
    }

    data class State(
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isConfirmButtonEnabled = seedPhraseState.isInputComplete()
    }
}
