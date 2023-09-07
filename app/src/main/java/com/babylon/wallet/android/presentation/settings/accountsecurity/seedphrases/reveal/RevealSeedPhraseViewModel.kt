package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource
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
                        body = FactorSource.HexCoded32Bytes(args.factorSourceId)
                    )
                ).getOrNull()?.let { mnemonicWithPassphrase ->
                    _state.update { state ->
                        state.copy(
                            mnemonicWords = mnemonicWithPassphrase
                                .mnemonic
                                .split(" ").chunked(state.seedPhraseWordsPerLine)
                                .map {
                                    it.toPersistentList()
                                }.toPersistentList(),
                            passphrase = mnemonicWithPassphrase.bip39Passphrase,
                            backedUp = backedUpIds.contains(args.factorSourceId)
                        )
                    }
                }
            }
        }
    }

    fun markFactorSourceBackedUp() {
        viewModelScope.launch {
            preferencesManager.markFactorSourceBackedUp(args.factorSourceId)
            sendEvent(Effect.Close)
        }
    }

    data class State(
        val mnemonicWords: PersistentList<PersistentList<String>> = persistentListOf(),
        val passphrase: String = "",
        val backedUp: Boolean = false,
        val seedPhraseWordsPerLine: Int = 3
    ) : UiState

    sealed interface Effect : OneOffEvent {
        object Close : Effect
    }
}
