package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.FactorSourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
class RevealSeedPhraseViewModel @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle,
    private val appEventBus: AppEventBus
) : StateViewModel<RevealSeedPhraseViewModel.State>(),
    OneOffEventHandler<RevealSeedPhraseViewModel.Effect> by OneOffEventHandlerImpl() {

    private val args = RevealSeedPhraseArgs(savedStateHandle)

    override fun initialState() = State()

    init {
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().collect { backedUpIds ->
                mnemonicRepository.readMnemonic(args.factorSourceId).onSuccess { mnemonicWithPassphrase ->
                    _state.update { state ->
                        state.copy(
                            mnemonicWordsChunked = mnemonicWithPassphrase
                                .mnemonic
                                .words
                                .map { it.word }
                                .chunked(state.seedPhraseWordsPerLine)
                                .map {
                                    it.toPersistentList()
                                }.toPersistentList(),
                            passphrase = mnemonicWithPassphrase.passphrase,
                            backedUp = backedUpIds.contains(args.factorSourceId),
                            mnemonicSize = mnemonicWithPassphrase.mnemonic.wordCount.value.toInt()
                        )
                    }
                }.onFailure { error ->
                    if (error is ProfileException.SecureStorageAccess) {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    }
                    _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(error))
                    }
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun showConfirmSeedPhraseDialog() {
        _state.update { state ->
            state.copy(
                showConfirmSeedPhraseDialogState = ConfirmSeedPhraseDialogState.Shown(args.factorSourceId, _state.value.mnemonicSize)
            )
        }
    }

    data class State(
        val mnemonicSize: Int = 0,
        val mnemonicWordsChunked: PersistentList<PersistentList<String>> = persistentListOf(),
        val passphrase: String = "",
        val backedUp: Boolean = false,
        val seedPhraseWordsPerLine: Int = 3,
        val showConfirmSeedPhraseDialogState: ConfirmSeedPhraseDialogState = ConfirmSeedPhraseDialogState.None,
        val uiMessage: UiMessage? = null,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState

    sealed interface ConfirmSeedPhraseDialogState {
        data object None : ConfirmSeedPhraseDialogState
        data class Shown(val factorSourceId: FactorSourceId.Hash, val mnemonicSize: Int) : ConfirmSeedPhraseDialogState
    }

    sealed interface Effect : OneOffEvent {
        data object Close : Effect
    }
}
