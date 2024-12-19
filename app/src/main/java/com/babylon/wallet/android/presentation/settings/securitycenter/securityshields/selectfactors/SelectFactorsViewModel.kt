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
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.name
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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

    private fun initFactorSources() {
        viewModelScope.launch {
            securityShieldBuilderClient.newSecurityShieldBuilder()

            _state.update { state ->
                state.copy(
                    status = securityShieldBuilderClient.validatePrimaryRoleFactorSourceSelection(),
                    items = securityShieldBuilderClient.getSortedPrimaryThresholdFactorSources()
                        .groupBy { it.kind }
                        .flatMap { entry ->
                            listOf(State.UiItem.CategoryHeader(entry.key)) + entry.value.map { it.toUiItem() }
                        }
                )
            }
        }
    }

    private fun FactorSource.toUiItem(): State.UiItem.Factor {
        return State.UiItem.Factor(
            Selectable(
                data = FactorSourceInstanceCard.compact(
                    id = id,
                    name = name,
                    kind = kind
                ),
                selected = false
            )
        )
    }

    fun onFactorCheckedChange(
        card: FactorSourceInstanceCard,
        checked: Boolean
    ) {
        viewModelScope.launch {
            val selectedFactorIds = securityShieldBuilderClient.updatePrimaryRoleThresholdFactorSourceSelection(card.id, checked)

            _state.update { state ->
                state.copy(
                    status = securityShieldBuilderClient.validatePrimaryRoleFactorSourceSelection(),
                    items = state.items.map { item ->
                        if (item is State.UiItem.Factor) {
                            item.copy(card = item.card.copy(selected = item.card.data.id in selectedFactorIds))
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    fun onBuildShieldClick() {
        viewModelScope.launch {
            securityShieldBuilderClient.autoAssignSelectedFactors()
                .onSuccess {
                    sendEvent(Event.ToRegularAccess)
                }
                .onFailure { error ->
                    Timber.e("Failed to auto-assign factors: ${error.message}")

                    _state.update { state ->
                        state.copy(
                            message = UiMessage.ErrorMessage(error)
                        )
                    }
                }
        }
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val status: SelectedFactorSourcesForRoleStatus? = null,
        val message: UiMessage? = null
    ) : UiState {

        val isButtonEnabled: Boolean = status == SelectedFactorSourcesForRoleStatus.OPTIMAL ||
            status == SelectedFactorSourcesForRoleStatus.SUBOPTIMAL

        fun showPasswordWarning(categoryHeader: UiItem.CategoryHeader): Boolean =
            categoryHeader.kind == FactorSourceKind.PASSWORD && items.any {
                (it as? UiItem.Factor)?.card?.data?.kind == FactorSourceKind.PASSWORD && it.card.selected
            } && status == SelectedFactorSourcesForRoleStatus.INVALID

        sealed interface UiItem {

            data class CategoryHeader(
                val kind: FactorSourceKind
            ) : UiItem

            data class Factor(
                val card: Selectable<FactorSourceInstanceCard>
            ) : UiItem
        }
    }

    sealed interface Event : OneOffEvent {

        data object ToRegularAccess : Event
    }
}
