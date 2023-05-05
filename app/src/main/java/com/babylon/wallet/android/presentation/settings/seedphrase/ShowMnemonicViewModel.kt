package com.babylon.wallet.android.presentation.settings.seedphrase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.utils.accountFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.deviceFactorSources
import javax.inject.Inject

@HiltViewModel
class ShowMnemonicViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ShowMnemonicViewModel.SeedPhraseUiState>(),
    OneOffEventHandler<ShowMnemonicViewModel.Effect> by OneOffEventHandlerImpl() {

    private val args = ShowMnemonicArgs(savedStateHandle)

    override fun initialState() = SeedPhraseUiState()

    init {
        viewModelScope.launch {
            getProfileUseCase.deviceFactorSources.collect { factorSources ->
                _state.update { it.copy(factorSources = factorSources.toPersistentList()) }
            }
        }
        args.factorSourceId?.let { factorSourceIdString ->
            onShowMnemonic(FactorSource.ID(factorSourceIdString))
        }
    }

    fun onShowMnemonic(factorSourceID: FactorSource.ID) {
        viewModelScope.launch {
            sendEvent(Effect.OnRequestToShowMnemonic(factorSourceID))
        }
    }

    fun onAuthenticationGrantedToShowMnemonic(factorSourceID: FactorSource.ID) {
        viewModelScope.launch {
            val mnemonic = mnemonicRepository.readMnemonic(factorSourceID)
            if (mnemonic != null) {
                _state.update { it.copy(visibleMnemonic = VisibleMnemonic.Shown(mnemonic = mnemonic, factorSourceID = factorSourceID)) }
            } else {
                val account = getProfileUseCase().first().currentNetwork.accounts.find { it.accountFactorSourceId() == factorSourceID }
                if (account != null) {
                    sendEvent(Effect.OnRequestToRecoverMnemonic(account.address))
                }
            }
        }
    }

    fun closeMnemonicDialog(backedUpFactorSourceId: FactorSource.ID?) {
        viewModelScope.launch {
            if (backedUpFactorSourceId != null) {
                preferencesManager.markFactorSourceBackedUp(backedUpFactorSourceId.value)
            }
            _state.update { it.copy(visibleMnemonic = VisibleMnemonic.None) }
        }
    }

    data class SeedPhraseUiState(
        val factorSources: ImmutableList<FactorSource> = persistentListOf(),
        val visibleMnemonic: VisibleMnemonic = VisibleMnemonic.None
    ) : UiState

    sealed interface Effect: OneOffEvent {
        data class OnRequestToShowMnemonic(val factorSourceID: FactorSource.ID): Effect
        data class OnRequestToRecoverMnemonic(val accountAddress: String): Effect
    }
}

sealed interface VisibleMnemonic {
    object None : VisibleMnemonic
    data class Shown(val mnemonic: MnemonicWithPassphrase, val factorSourceID: FactorSource.ID) : VisibleMnemonic
}
