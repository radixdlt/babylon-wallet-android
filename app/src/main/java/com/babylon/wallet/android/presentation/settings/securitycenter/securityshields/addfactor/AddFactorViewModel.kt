package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SecurityShieldPrerequisitesStatus
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

@HiltViewModel
class AddFactorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val securityShieldBuilderClient: SecurityShieldBuilderClient,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<AddFactorViewModel.State>(),
    OneOffEventHandler<AddFactorViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AddFactorScreenArgs.from(savedStateHandle)

    override fun initialState(): State = State(mode = State.Mode.from(args))

    fun onFactorSourceKindSelect(card: FactorSourceKindCard) {
        _state.update { it.copy(selected = card.kind) }
    }

    fun onButtonClick() {
        val selectedKind = state.value.selected ?: error("No factor source selected")
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(
                AddFactorSourceInput.WithKind(
                    kind = selectedKind,
                    context = AddFactorSourceInput.Context.New
                )
            )
            checkShieldPrerequisites()
        }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    fun onSkipClick() {
        viewModelScope.launch {
            securityShieldBuilderClient.newSecurityShieldBuilder()
            sendEvent(Event.ToRegularAccess)
        }
    }

    private fun checkShieldPrerequisites() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                securityShieldPrerequisitesStatus()
            }.onSuccess { status ->
                when (status) {
                    SecurityShieldPrerequisitesStatus.SUFFICIENT -> sendEvent(Event.FactorReady)
                    SecurityShieldPrerequisitesStatus.HARDWARE_REQUIRED -> sendEvent(Event.AddHardwareDevice)
                    SecurityShieldPrerequisitesStatus.ANY_REQUIRED -> sendEvent(Event.AddAnotherFactor)
                }
            }.onFailure {
                _state.update { state -> state.copy(message = UiMessage.ErrorMessage(it)) }
            }
        }
    }

    data class State(
        val mode: Mode,
        val message: UiMessage? = null,
        val selected: FactorSourceKind? = null
    ) : UiState {

        val factorSourceKinds: List<Selectable<FactorSourceKindCard>> = mode.kinds.map { kind ->
            Selectable(
                data = FactorSourceKindCard(
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

        data object AddHardwareDevice : Event

        data object AddAnotherFactor : Event

        data object ToRegularAccess : Event

        data object FactorReady : Event
    }
}
