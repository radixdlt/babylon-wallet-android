package com.babylon.wallet.android.presentation.settings.recovery.chooseseed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.DeviceFactorSourceData
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.babylonFactorSourcesWithAccounts
import rdx.works.profile.domain.olympiaFactorSourcesWithAccounts
import javax.inject.Inject

@HiltViewModel
class ChooseSeedPhraseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) :
    StateViewModel<ChooseSeedPhraseViewModel.State>(),
    OneOffEventHandler<ChooseSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(mnemonicType = args.recoveryType)

    private val args = ChooseSeedPhraseArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            when (args.recoveryType) {
                MnemonicType.BabylonMain,
                MnemonicType.Babylon -> getProfileUseCase.babylonFactorSourcesWithAccounts
                MnemonicType.Olympia -> getProfileUseCase.olympiaFactorSourcesWithAccounts
            }.collect { factorSources ->
                val existing = _state.value.factorSources
                var updated = factorSources.map { entry ->
                    val data = DeviceFactorSourceData(
                        deviceFactorSource = entry.key,
                        accounts = entry.value.toPersistentList()
                    )
                    Selectable(data, selected = existing.any { it.data.deviceFactorSource.id == entry.key.id && it.selected })
                }
                if (updated.size == 1) {
                    updated = updated.map { it.copy(selected = true) }
                }
                _state.update { it.copy(factorSources = updated.toPersistentList()) }
            }
        }
    }

    fun onSelectionChanged(id: FactorSource.FactorSourceID.FromHash) {
        _state.update { state ->
            val updated = state.factorSources.map { selectable ->
                selectable.copy(selected = selectable.data.deviceFactorSource.id == id)
            }
            state.copy(factorSources = updated.toPersistentList())
        }
    }

    fun onUseFactorSource() {
        _state.value.selectedFactorSource?.let { factorSource ->
            viewModelScope.launch {
                sendEvent(Event.UseFactorSource(factorSource))
            }
        }
    }

    data class State(
        val factorSources: ImmutableList<Selectable<DeviceFactorSourceData>> = persistentListOf(),
        val uiMessage: UiMessage? = null,
        val mnemonicType: MnemonicType = MnemonicType.Babylon
    ) : UiState {
        val selectedFactorSource
            get() = factorSources.firstOrNull { it.selected }?.data?.deviceFactorSource
    }

    sealed interface Event : OneOffEvent {
        data class UseFactorSource(val factorSource: DeviceFactorSource) : Event
    }
}
