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
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorListKind
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatusInvalidReason
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectFactorsViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SelectFactorsViewModel.State>(),
    OneOffEventHandler<SelectFactorsViewModel.Event> by OneOffEventHandlerImpl() {

    private var hasMadeFirstSelection: Boolean = false

    init {
        initFactorSources()
    }

    override fun initialState(): State = State()

    fun onFactorCheckedChange(
        card: FactorSourceCard,
        checked: Boolean
    ) {
        hasMadeFirstSelection = true

        viewModelScope.launch {
            securityShieldBuilderClient.executeMutatingFunction {
                if (checked) {
                    addFactorSourceToPrimaryThreshold(card.id)
                } else {
                    removeFactorFromPrimary(card.id, FactorListKind.THRESHOLD)
                }
            }
        }
    }

    fun onBuildShieldClick() {
        viewModelScope.launch {
            securityShieldBuilderClient.autoAssignFactors()
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
                            status = it.primaryRoleStatus.takeIf { hasMadeFirstSelection },
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
                data = toFactorSourceCard(),
                selected = false
            )
        )
    }

    fun onSkipClick() {
        viewModelScope.launch {
            securityShieldBuilderClient.newSecurityShieldBuilder()
            sendEvent(Event.ToRegularAccess)
        }
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val status: SelectedPrimaryThresholdFactorsStatus? = null,
        val message: UiMessage? = null
    ) : UiState {

        val isButtonEnabled: Boolean = status != null && status != SelectedPrimaryThresholdFactorsStatus.Insufficient

        fun cannotBeUsedByItself(categoryHeader: UiItem.CategoryHeader): Boolean =
            (status as? SelectedPrimaryThresholdFactorsStatus.Invalid)?.let { status ->
                val cannotBeUsedAlone = (status.reason as? SelectedPrimaryThresholdFactorsStatusInvalidReason.CannotBeUsedAlone)
                cannotBeUsedAlone?.factorSourceKind == categoryHeader.kind
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
