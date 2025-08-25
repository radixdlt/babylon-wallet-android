package com.babylon.wallet.android.presentation.addfactorsource.kind

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceOutput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFactorSourceKindViewModel @Inject constructor(
    private val addFactorSourceProxy: AddFactorSourceProxy,
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler
) : StateViewModel<AddFactorSourceKindViewModel.State>(),
    OneOffEventHandler<AddFactorSourceKindViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput()

    init {
        _state.update { state ->
            state.copy(
                isLoading = false,
                items = FactorSourceKind.entries.filter {
                    when (input) {
                        is AddFactorSourceInput.SelectKind -> it in input.kinds
                        AddFactorSourceInput.Init,
                        is AddFactorSourceInput.WithKind -> error("Shouldn't be here")
                    }
                }.map {
                    Selectable(
                        data = FactorSourceKindCard(
                            kind = it,
                            messages = persistentListOf()
                        )
                    )
                }
            )
        }
    }

    override fun initialState(): State = State(isLoading = true)

    fun onSelectFactorSourceKindCard(card: FactorSourceKindCard) {
        _state.update { state ->
            state.copy(
                items = state.items.map { item ->
                    item.copy(
                        selected = item.data.kind == card.kind
                    )
                }
            )
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            addFactorSourceIOHandler.setOutput(AddFactorSourceOutput.Init)
            sendEvent(Event.Dismiss)
        }
    }

    fun onAddClick() {
        val selectedItem = checkNotNull(state.value.selectedItem)

        viewModelScope.launch {
            val factorSourceId = addFactorSourceProxy.addFactorSource(
                AddFactorSourceInput.WithKind(selectedItem.kind, input.context())
            )?.value ?: return@launch

            addFactorSourceIOHandler.setOutput(AddFactorSourceOutput.Id(factorSourceId))
            sendEvent(Event.Complete(selectedItem.kind))
        }
    }

    data class State(
        val isLoading: Boolean,
        val items: List<Selectable<FactorSourceKindCard>> = emptyList()
    ) : UiState {

        val selectedItem = items.firstOrNull { it.selected }?.data
        val isButtonEnabled: Boolean = selectedItem != null
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event

        data class Complete(
            val factorSourceKind: FactorSourceKind
        ) : Event
    }
}
