package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseVerificationDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
class ConfirmSeedPhraseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    private val appEventBus: AppEventBus
) : StateViewModel<ConfirmSeedPhraseViewModel.State>(),
    OneOffEventHandler<ConfirmSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ConfirmSeedPhraseArgs(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseVerificationDelegate(viewModelScope)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSource = requireNotNull(getProfileUseCase().factorSourceById(args.factorSourceId) as? FactorSource.Device)
            val mnemonicExist = mnemonicRepository.mnemonicExist(factorSource.value.id.asGeneral())
            if (mnemonicExist) {
                seedPhraseInputDelegate.init(args.mnemonicSize)
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
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onSubmit() {
        viewModelScope.launch {
            val factorSource = requireNotNull(getProfileUseCase().factorSourceById(args.factorSourceId) as? FactorSource.Device)
            mnemonicRepository.readMnemonic(factorSource.value.id.asGeneral()).onSuccess { mnemonicWithPassphrase ->
                val mnemonicWords = mnemonicWithPassphrase.mnemonic.words
                val inputMatchesMnemonic =
                    _state.value.seedPhraseState.wordsToConfirm.all { entry -> mnemonicWords[entry.key].word == entry.value }
                if (inputMatchesMnemonic) {
                    preferencesManager.markFactorSourceBackedUp(factorSource.value.id.asGeneral())
                    sendEvent(Event.MnemonicBackedUp)
                } else {
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(ProfileException.InvalidMnemonic)) }
                }
            }.onFailure { e ->
                if (e is ProfileException.SecureStorageAccess) {
                    appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                } else {
                    _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(e)) }
                }
            }
        }
    }

    data class State(
        private val factorSource: FactorSource? = null,
        val uiMessage: UiMessage? = null,
        val seedPhraseState: SeedPhraseVerificationDelegate.State = SeedPhraseVerificationDelegate.State()
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object MnemonicBackedUp : Event
    }
}
