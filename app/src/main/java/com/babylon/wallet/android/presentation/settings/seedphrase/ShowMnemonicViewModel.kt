package com.babylon.wallet.android.presentation.settings.seedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.BaseViewModel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSources
import javax.inject.Inject

@HiltViewModel
class ShowMnemonicViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository
) : BaseViewModel<ShowMnemonicViewModel.SeedPhraseUiState>(), OneOffEventHandler<ShowMnemonicEvent> by OneOffEventHandlerImpl() {

    override fun initialState() = SeedPhraseUiState()

    init {
        viewModelScope.launch {
            getProfileUseCase.factorSources.collect { factorSources ->
                _state.update { it.copy(factorSources = factorSources.toPersistentList()) }
            }
        }
    }

    fun onShowMnemonic(factorSourceID: FactorSource.ID) {
        viewModelScope.launch {
            mnemonicRepository.readMnemonic(factorSourceID)?.let { mnemonic ->
                sendEvent(ShowMnemonicEvent.ShowMnemonic(mnemonic))
            }
        }
    }

    data class SeedPhraseUiState(
        val factorSources: ImmutableList<FactorSource> = persistentListOf()
    ) : UiState
}

sealed interface ShowMnemonicEvent : OneOffEvent {
    data class ShowMnemonic(val mnemonic: MnemonicWithPassphrase?) : ShowMnemonicEvent
}
