package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common.toCompactInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupRegularAccessViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SetupRegularAccessViewModel.State>() {

    init {
        observeSelection()
    }

    override fun initialState(): State = State()

    fun onNumberOfFactorsClick() {
        val factorCount = _state.value.thresholdFactors.size

        _state.update {
            it.copy(
                selectNumberOfFactors = State.SelectNumberOfFactors(
                    current = it.numberOfFactors,
                    items = (
                        listOf(State.NumberOfFactors.All) + (factorCount - 1 downTo 1)
                            .map { value -> State.NumberOfFactors.Count(value) }
                        )
                        .toPersistentList()
                )
            )
        }
    }

    fun onNumberOfFactorsSelect(numberOfFactors: State.NumberOfFactors) {
        viewModelScope.launch {
            securityShieldBuilderClient.setThreshold(numberOfFactors.toThreshold(_state.value.thresholdFactors.size))
        }
    }

    fun onNumberOfFactorsSelectionDismiss() {
        _state.update { it.copy(selectNumberOfFactors = null) }
    }

    fun onAddThresholdFactorClick() {
        _state.update { state ->
            state.copy(
                selectFactor = State.SelectFactor(
                    purpose = State.SelectFactor.Purpose.Threshold,
                    excludeFactorSources = state.thresholdFactors.map { it.id }.toPersistentList()
                )
            )
        }
    }

    fun onRemoveThresholdFactorClick(card: FactorSourceCard) {
        viewModelScope.launch { securityShieldBuilderClient.removeFactorsFromPrimary(listOf(card.id)) }
    }

    fun onAddOverrideClick() {
        _state.update { state ->
            state.copy(
                selectFactor = State.SelectFactor(
                    purpose = State.SelectFactor.Purpose.Override,
                    excludeFactorSources = state.overrideFactors.map { it.id }.toPersistentList()
                )
            )
        }
    }

    fun onAddAuthenticationFactorClick() {
        _state.update { state ->
            state.copy(
                selectFactor = State.SelectFactor(
                    purpose = State.SelectFactor.Purpose.Authentication,
                    excludeFactorSources = persistentListOf()
                )
            )
        }
    }

    fun onFactorSelected(card: FactorSourceCard) {
        val selectFactor = _state.value.selectFactor ?: return

        viewModelScope.launch {
            when (selectFactor.purpose) {
                State.SelectFactor.Purpose.Threshold -> securityShieldBuilderClient.updatePrimaryRoleThresholdFactorSourceSelection(
                    id = card.id,
                    isSelected = true
                )
                State.SelectFactor.Purpose.Override -> securityShieldBuilderClient.addPrimaryRoleOverrideFactorSource(card.id)
                State.SelectFactor.Purpose.Authentication -> securityShieldBuilderClient.setAuthenticationFactor(card.id)
            }
        }
    }

    fun onDismissSelectFactor() {
        _state.update { state -> state.copy(selectFactor = null) }
    }

    fun onRemoveAuthenticationFactorClick() {
        viewModelScope.launch { securityShieldBuilderClient.setAuthenticationFactor(null) }
    }

    fun onRemoveOverrideFactorClick(card: FactorSourceCard) {
        viewModelScope.launch { securityShieldBuilderClient.removeFactorsFromPrimary(listOf(card.id)) }
    }

    fun onRemoveAllOverrideFactorsClick() {
        viewModelScope.launch {
            val ids = _state.value.overrideFactors.map { it.id }
            securityShieldBuilderClient.removeFactorsFromPrimary(ids)
        }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.primaryRoleSelection()
                .collect { selection ->
                    _state.update { state ->
                        state.copy(
                            thresholdFactors = selection.thresholdFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                            overrideFactors = selection.overrideFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                            authenticationFactor = selection.authenticationFactor?.toCompactInstanceCard(true),
                            status = selection.shieldStatus,
                            numberOfFactors = State.NumberOfFactors.fromThreshold(selection.threshold, selection.thresholdFactors.size),
                            selectNumberOfFactors = null,
                            selectFactor = null
                        )
                    }
                }
        }
    }

    data class State(
        val status: SecurityShieldBuilderInvalidReason? = null,
        val numberOfFactors: NumberOfFactors = NumberOfFactors.All,
        val selectNumberOfFactors: SelectNumberOfFactors? = null,
        val thresholdFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val overrideFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val authenticationFactor: FactorSourceCard? = null,
        val message: UiMessage? = null,
        val selectFactor: SelectFactor? = null
    ) : UiState {

        data class SelectFactor(
            val purpose: Purpose,
            val excludeFactorSources: PersistentList<FactorSourceId>
        ) {

            enum class Purpose {
                Threshold,
                Override,
                Authentication
            }
        }

        data class SelectNumberOfFactors(
            val current: NumberOfFactors,
            val items: PersistentList<NumberOfFactors>
        )

        sealed interface NumberOfFactors {

            data object All : NumberOfFactors

            data class Count(
                val value: Int
            ) : NumberOfFactors

            fun toThreshold(factorCount: Int): Int = when (this) {
                is All -> factorCount
                is Count -> value
            }

            companion object {

                fun fromThreshold(threshold: Int, factorCount: Int): NumberOfFactors = when {
                    threshold == factorCount -> All
                    else -> Count(threshold)
                }
            }
        }
    }
}
