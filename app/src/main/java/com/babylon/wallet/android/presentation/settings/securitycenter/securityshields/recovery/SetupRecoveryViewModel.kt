package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common.toCompactInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupRecoveryViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SetupRecoveryViewModel.State>() {

    init {
        initSelection()
    }

    override fun initialState(): State = State()

    fun onAddStartRecoveryFactorClick() {
        // Show factor source selector
    }

    fun onRemoveStartRecoveryFactor(card: FactorSourceCard) {
        viewModelScope.launch { securityShieldBuilderClient.removeFactorFromRecovery(card.id) }
    }

    fun onAddConfirmRecoveryFactorClick() {
        // Show factor source selector
    }

    fun onRemoveConfirmRecoveryFactor(card: FactorSourceCard) {
        viewModelScope.launch {
            securityShieldBuilderClient.removeFactorFromConfirmation(card.id)
            initSelection()
        }
    }

    fun onContinueClick() {
        // Show shield name sheet
    }

    private fun initSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.recoveryRoleSelection()
                .collect { selection ->
                    _state.update { state ->
                        state.copy(
                            startFactors = selection.startRecoveryFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                            confirmFactors = selection.confirmationFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                            status = selection.shieldStatus,
                            fallbackPeriod = if (selection.numberOfDaysUntilAutoConfirm % DAYS_IN_A_WEEK == 0) {
                                State.FallbackPeriod(
                                    value = selection.numberOfDaysUntilAutoConfirm / DAYS_IN_A_WEEK,
                                    unit = State.FallbackPeriod.Unit.WEEKS
                                )
                            } else {
                                State.FallbackPeriod(
                                    value = selection.numberOfDaysUntilAutoConfirm,
                                    unit = State.FallbackPeriod.Unit.DAYS
                                )
                            },
                            selectFallbackPeriod = null
                        )
                    }
                }
        }
    }

    fun onFallbackPeriodClick() {
        _state.update { state ->
            state.copy(
                selectFallbackPeriod = State.SelectFallbackPeriod(
                    currentValue = requireNotNull(state.fallbackPeriod).value,
                    currentUnit = state.fallbackPeriod.unit,
                    values = state.fallbackPeriod.unit.possibleValues,
                    units = State.FallbackPeriod.Unit.entries.toPersistentList()
                )
            )
        }
    }

    fun onSetFallbackPeriodClick() {
        viewModelScope.launch {
            val fallbackPeriod = requireNotNull(state.value.selectFallbackPeriod)
            securityShieldBuilderClient.setNumberOfDaysUntilAutoConfirm(fallbackPeriod.currentUnit.toDays(fallbackPeriod.currentValue))
        }
    }

    fun onDismissFallbackPeriod() {
        _state.update { state -> state.copy(selectFallbackPeriod = null) }
    }

    fun onFallbackPeriodValueChange(value: Int) {
        _state.update { state ->
            state.copy(
                selectFallbackPeriod = state.selectFallbackPeriod?.copy(
                    currentValue = value
                )
            )
        }
    }

    fun onFallbackPeriodUnitChange(unit: State.FallbackPeriod.Unit) {
        _state.update { state ->
            val newValues = unit.possibleValues
            state.copy(
                selectFallbackPeriod = state.selectFallbackPeriod?.copy(
                    currentUnit = unit,
                    values = newValues,
                    currentValue = if (state.selectFallbackPeriod.currentValue in newValues) {
                        state.selectFallbackPeriod.currentValue
                    } else {
                        newValues.first()
                    }
                )
            )
        }
    }

    data class State(
        val status: SecurityShieldBuilderInvalidReason? = null,
        val startFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val confirmFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val fallbackPeriod: FallbackPeriod? = null,
        val selectFallbackPeriod: SelectFallbackPeriod? = null
    ) : UiState {

        val isButtonEnabled = status == null

        data class SelectFallbackPeriod(
            val currentValue: Int,
            val currentUnit: FallbackPeriod.Unit,
            val values: PersistentList<Int>,
            val units: PersistentList<FallbackPeriod.Unit>
        )

        data class FallbackPeriod(
            val value: Int,
            val unit: Unit
        ) {

            enum class Unit(
                val possibleValues: PersistentList<Int>
            ) {

                DAYS(FALLBACK_PERIOD_DAYS),
                WEEKS(FALLBACK_PERIOD_WEEKS);

                fun toDays(value: Int): Int = when (this) {
                    DAYS -> value
                    WEEKS -> value * DAYS_IN_A_WEEK
                }
            }
        }
    }

    companion object {

        private const val DAYS_IN_A_WEEK = 7
        private val FALLBACK_PERIOD_DAYS = (1 until 15).toPersistentList()
        private val FALLBACK_PERIOD_WEEKS = (1 until 5).toPersistentList()
    }
}
