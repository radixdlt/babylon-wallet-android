package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceByIdValue
import javax.inject.Inject

@HiltViewModel
class ConfirmMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) : StateViewModel<ConfirmMnemonicViewModel.State>(),
    OneOffEventHandler<ConfirmMnemonicViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ConfirmSeedPhraseArgs(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSource = requireNotNull(getProfileUseCase.factorSourceByIdValue(args.factorSourceId) as? DeviceFactorSource)
            val mnemonicExist = mnemonicRepository.mnemonicExist(factorSource.id)
            if (mnemonicExist) {
                seedPhraseInputDelegate.initInConfirmMode(args.mnemonicSize)
            }
        }
        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { it.copy(seedPhraseState = delegateState) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value) {
            sendEvent(Event.MoveToNextWord)
        }
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
        viewModelScope.launch {
            sendEvent(Event.MoveToNextWord)
        }
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onSubmit() {
        viewModelScope.launch {
            val factorSource = requireNotNull(getProfileUseCase.factorSourceByIdValue(args.factorSourceId) as? DeviceFactorSource)
            mnemonicRepository.readMnemonic(factorSource.id).onSuccess { mnemonicWithPassphrase ->
                val mnemonicWords = mnemonicWithPassphrase.words
                val inputMatchesMnemonic =
                    _state.value.seedPhraseState.wordsToConfirm.all { entry -> mnemonicWords[entry.key] == entry.value }
                if (inputMatchesMnemonic) {
                    preferencesManager.markFactorSourceBackedUp(args.factorSourceId)
                    sendEvent(Event.MnemonicBackedUp)
                } else {
                    _state.update { it.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic) }
                }
            }
        }
    }

    data class State(
        private val factorSource: FactorSource? = null,
        val uiMessage: UiMessage? = null,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object MoveToNextWord : Event
        data object MnemonicBackedUp : Event
    }
}
