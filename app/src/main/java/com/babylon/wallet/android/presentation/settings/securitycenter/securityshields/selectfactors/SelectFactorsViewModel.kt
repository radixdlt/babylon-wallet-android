package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.domain.model.Selectable
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
import javax.inject.Inject

@HiltViewModel
class SelectFactorsViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SelectFactorsViewModel.State>() {

    init {
        initFactorSources()
    }

    override fun initialState(): State = State()

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    private fun initFactorSources() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    status = securityShieldBuilderClient.validateFactorSourceSelection(),
                    items = securityShieldBuilderClient.getFactorSources()
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
        val selectedFactorIds = securityShieldBuilderClient.updateFactorSourceSelection(card.id, checked)

        _state.update { state ->
            state.copy(
                status = securityShieldBuilderClient.validateFactorSourceSelection(),
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

    data class State(
        val items: List<UiItem> = emptyList(),
        val status: SelectedFactorSourcesForRoleStatus? = null,
        val message: UiMessage? = null
    ) : UiState {

        val isButtonEnabled: Boolean = items.any { it is UiItem.Factor && it.card.selected }

        fun showPasswordWarning(categoryHeader: UiItem.CategoryHeader): Boolean =
            categoryHeader.kind == FactorSourceKind.PASSWORD && items.any {
                (it as? UiItem.Factor)?.card?.data?.kind == FactorSourceKind.PASSWORD && it.card.selected
            }

        sealed interface UiItem {

            data class CategoryHeader(
                val kind: FactorSourceKind
            ) : UiItem

            data class Factor(
                val card: Selectable<FactorSourceInstanceCard>
            ) : UiItem
        }
    }
}
