package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Bip39WordCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
class AddSingleMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val appEventBus: AppEventBus
) : StateViewModel<AddSingleMnemonicViewModel.State>(),
    OneOffEventHandler<AddSingleMnemonicViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AddSingleMnemonicNavArgs(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State(mnemonicType = args.mnemonicType)

    init {
        seedPhraseInputDelegate.setSeedPhraseSize(
            size = when (args.mnemonicType) {
                MnemonicType.BabylonMain -> Bip39WordCount.TWENTY_FOUR
                MnemonicType.Babylon -> Bip39WordCount.TWENTY_FOUR
                MnemonicType.Olympia -> Bip39WordCount.TWELVE
            }
        )
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
        seedPhraseInputDelegate.onWordChanged(index, value)
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

    fun onSeedPhraseLengthChanged(value: Bip39WordCount) {
        seedPhraseInputDelegate.setSeedPhraseSize(value)
    }

    fun onAddFactorSource() {
        viewModelScope.launch {
            val mnemonic = _state.value.seedPhraseState.toMnemonicWithPassphrase()
            when (args.mnemonicType) {
                MnemonicType.Babylon -> {
                    ensureBabylonFactorSourceExistUseCase.addBabylonFactorSource(mnemonic).onSuccess {
                        sendEvent(Event.FactorSourceAdded)
                    }.onFailure { error ->
                        if (error is ProfileException.SecureStorageAccess) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        } else {
                            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                        }
                    }
                }

                MnemonicType.Olympia -> {
                    addOlympiaFactorSourceUseCase(mnemonic).onSuccess {
                        sendEvent(Event.FactorSourceAdded)
                    }.onFailure { error ->
                        if (error is ProfileException.SecureStorageAccess) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        }
                        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                    }
                }

                MnemonicType.BabylonMain -> {}
            }
        }
    }

    fun onAddMainSeedPhrase() {
        viewModelScope.launch {
            accessFactorSourcesProxy.setTempMnemonicWithPassphrase(
                mnemonicWithPassphrase = state.value.seedPhraseState.toMnemonicWithPassphrase()
            )
            sendEvent(Event.MainSeedPhraseCompleted)
        }
    }

    data class State(
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val mnemonicType: MnemonicType = MnemonicType.Babylon,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object MoveToNextWord : Event
        data object FactorSourceAdded : Event
        data object MainSeedPhraseCompleted : Event
    }
}
