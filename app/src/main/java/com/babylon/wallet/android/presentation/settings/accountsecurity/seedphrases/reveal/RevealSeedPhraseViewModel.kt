package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

@HiltViewModel
class RevealSeedPhraseViewModel @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : StateViewModel<RevealSeedPhraseViewModel.State>(),
    OneOffEventHandler<RevealSeedPhraseViewModel.Effect> by OneOffEventHandlerImpl() {

    private val args = RevealSeedPhraseArgs(savedStateHandle)

    override fun initialState() = State()

    init {
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().collect { backedUpIds ->
                mnemonicRepository.readMnemonic(
                    FactorSourceID.FromHash(
                        kind = FactorSourceKind.DEVICE,
                        body = HexCoded32Bytes(args.factorSourceId)
                    )
                ).getOrNull()?.let { mnemonicWithPassphrase ->
                    _state.update { state ->
                        state.copy(
                            mnemonicWordsChunked = mnemonicWithPassphrase
                                .mnemonic
                                .split(" ").chunked(state.seedPhraseWordsPerLine)
                                .map {
                                    it.toPersistentList()
                                }.toPersistentList(),
                            passphrase = mnemonicWithPassphrase.bip39Passphrase,
                            backedUp = backedUpIds.contains(args.factorSourceId),
                            mnemonicSize = mnemonicWithPassphrase.wordCount
                        )
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
        data class Shown(val factorSourceId: String, val mnemonicSize: Int) : ConfirmSeedPhraseDialogState
    }

    sealed interface Effect : OneOffEvent {
        data object Close : Effect
    }
}
