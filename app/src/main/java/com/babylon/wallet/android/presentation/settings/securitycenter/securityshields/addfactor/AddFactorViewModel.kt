package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFactorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : StateViewModel<AddFactorViewModel.State>(),
    OneOffEventHandler<AddFactorViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AddFactorScreenArgs.from(savedStateHandle)

    override fun initialState(): State = State(mode = State.Mode.from(args))

    fun onFactorSourceKindSelect(card: FactorSourceKindCard) {
        _state.update { it.copy(selected = card.kind) }
    }

    fun onButtonClick() {
        val selectedKind = state.value.selected ?: error("No factor source selected")
        viewModelScope.launch { sendEvent(Event.ToFactorSetup(selectedKind)) }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val mode: Mode,
        val message: UiMessage? = null,
        val selected: FactorSourceKind? = null
    ) : UiState {

        val factorSources: List<Selectable<FactorSourceCard>> = mode.kinds.map { kind ->
            Selectable(
                data = FactorSourceCard(
                    kind = kind,
                    messages = listOfNotNull(
                        FactorSourceStatusMessage.PassphraseHint.takeIf {
                            kind == selected && kind == FactorSourceKind.OFF_DEVICE_MNEMONIC
                        }
                    ).toPersistentList()
                ),
                selected = kind == selected
            )
        }

        val factorSourceKinds: List<FactorSourceKindCard> = mode.kinds.map { kind ->
            FactorSourceKindCard(
                kind = kind,
                messages = listOfNotNull(
                    FactorSourceStatusMessage.PassphraseHint.takeIf {
                        kind == selected && kind == FactorSourceKind.OFF_DEVICE_MNEMONIC
                    }
                ).toPersistentList()
            )
        }

        val isModeHardwareOnly = mode == Mode.HARDWARE_ONLY

        val isButtonEnabled = selected != null

        enum class Mode(
            val kinds: List<FactorSourceKind>
        ) {

            HARDWARE_ONLY(
                listOf(
                    FactorSourceKind.ARCULUS_CARD,
                    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
                )
            ),
            ANY(
                listOf(
                    FactorSourceKind.PASSWORD,
                    FactorSourceKind.ARCULUS_CARD,
                    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    FactorSourceKind.OFF_DEVICE_MNEMONIC
                )
            );

            companion object {

                fun from(args: AddFactorScreenArgs): Mode {
                    return when (args.mode) {
                        AddFactorScreenArgs.Mode.HARDWARE_ONLY -> HARDWARE_ONLY
                        AddFactorScreenArgs.Mode.ANY -> ANY
                    }
                }
            }
        }
    }

    sealed interface Event : OneOffEvent {

        data class ToFactorSetup(
            val kind: FactorSourceKind
        ) : Event
    }
}
