package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatusInvalidReason
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.name
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectFactorsViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SelectFactorsViewModel.State>(),
    OneOffEventHandler<SelectFactorsViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        initFactorSources()
    }

    override fun initialState(): State = State()

    fun onFactorCheckedChange(
        card: FactorSourceCard,
        checked: Boolean
    ) {
        viewModelScope.launch {
            securityShieldBuilderClient.updatePrimaryRoleThresholdFactorSourceSelection(card.id, checked)
        }
    }

    fun onBuildShieldClick() {
        viewModelScope.launch {
            securityShieldBuilderClient.autoAssignSelectedFactors()
            sendEvent(Event.ToRegularAccess)
        }
    }

    private fun initFactorSources() {
        viewModelScope.launch {
            securityShieldBuilderClient.newSecurityShieldBuilder()

            _state.update { state ->
                state.copy(
                    items = securityShieldBuilderClient.getSortedPrimaryThresholdFactorSources()
                        .groupBy { it.kind }
                        .flatMap { entry ->
                            listOf(State.UiItem.CategoryHeader(entry.key)) + entry.value.map { it.toUiItem() }
                        }
                )
            }

            observeSelection()
        }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.primaryRoleSelection()
                .collect {
                    _state.update { state ->
                        state.copy(
                            status = it.primaryRoleStatus,
                            items = state.items.map { item ->
                                if (item is State.UiItem.Factor) {
                                    item.copy(
                                        card = item.card.copy(
                                            selected = item.card.data.id in it.thresholdFactors.map { factor -> factor.id }
                                        )
                                    )
                                } else {
                                    item
                                }
                            }
                        )
                    }
                }
        }
    }

    private fun FactorSource.toUiItem(): State.UiItem.Factor {
        return State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = id,
                    name = name,
                    kind = kind
                ),
                selected = false
            )
        )
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val status: SelectedPrimaryThresholdFactorsStatus? = null,
        val message: UiMessage? = null
    ) : UiState {

        val isButtonEnabled: Boolean = status == SelectedPrimaryThresholdFactorsStatus.Optimal ||
            status == SelectedPrimaryThresholdFactorsStatus.Suboptimal

        fun cannotBeUsedByItself(categoryHeader: UiItem.CategoryHeader): Boolean =
            (status as? SelectedPrimaryThresholdFactorsStatus.Invalid)?.let { status ->
                (status.reason as? SelectedPrimaryThresholdFactorsStatusInvalidReason.CannotBeUsedAlone)?.factorSourceKind == categoryHeader.kind
            } ?: false

        sealed interface UiItem {

            data class CategoryHeader(
                val kind: FactorSourceKind
            ) : UiItem

            data class Factor(
                val card: Selectable<FactorSourceCard>
            ) : UiItem
        }
    }

    sealed interface Event : OneOffEvent {

        data object ToRegularAccess : Event
    }
}
