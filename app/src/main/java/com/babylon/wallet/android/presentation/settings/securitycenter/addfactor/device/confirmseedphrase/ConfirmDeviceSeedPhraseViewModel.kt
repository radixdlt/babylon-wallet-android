package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.DeviceFactorSourceAddingClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.DeviceMnemonicBuildOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import javax.inject.Inject

@HiltViewModel
class ConfirmDeviceSeedPhraseViewModel @Inject constructor(
    private val deviceFactorSourceAddingClient: DeviceFactorSourceAddingClient
) : StateViewModel<ConfirmDeviceSeedPhraseViewModel.State>(),
    OneOffEventHandler<ConfirmDeviceSeedPhraseViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    words = deviceFactorSourceAddingClient.generateConfirmationWords().toPersistentList()
                )
            }
        }
    }

    override fun initialState(): State = State()

    fun onWordChanged(index: Int, value: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    words = state.words.mapWhen(
                        predicate = { it.index == index },
                        mutation = { it.copy(value = value.trim()) }
                    ).toPersistentList()
                )
            }
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            when (val outcome = deviceFactorSourceAddingClient.confirmWords(state.value.words)) {
                is DeviceMnemonicBuildOutcome.Confirmed -> sendEvent(Event.Confirmed)
                is DeviceMnemonicBuildOutcome.Unconfirmed -> {
                    val incorrectIndices = outcome.indicesInMnemonic.map { it.toInt() }

                    _state.update { state ->
                        state.copy(
                            words = state.words.mapWhen(
                                predicate = { it.index in incorrectIndices },
                                mutation = { it.copy(state = SeedPhraseWord.State.Invalid) }
                            ).toPersistentList()
                        )
                    }
                }
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data object Confirmed : Event
    }

    data class State(
        val words: ImmutableList<SeedPhraseWord> = persistentListOf(),
    ) : UiState {

        val isConfirmButtonEnabled = words.all { it.hasValue }
    }
}