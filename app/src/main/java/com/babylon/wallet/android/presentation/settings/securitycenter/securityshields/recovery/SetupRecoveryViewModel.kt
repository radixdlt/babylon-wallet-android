package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.TimePeriodUnit
import com.radixdlt.sargon.extensions.values
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
) : StateViewModel<SetupRecoveryViewModel.State>(),
    OneOffEventHandler<SetupRecoveryViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        initSelection()
    }

    override fun initialState(): State = State()

    fun onAddStartRecoveryFactorClick() {
        _state.update { state ->
            state.copy(
                selectFactor = State.SelectFactor(
                    purpose = State.SelectFactor.Purpose.StartRecovery,
                    excludeFactorSources = state.startRecoveryFactors.map { it.id }.toPersistentList()
                )
            )
        }
    }

    fun onRemoveStartRecoveryFactor(card: FactorSourceCard) {
        viewModelScope.launch { securityShieldBuilderClient.removeFactorSourceFromRecovery(card.id) }
    }

    fun onAddConfirmRecoveryFactorClick() {
        _state.update { state ->
            state.copy(
                selectFactor = State.SelectFactor(
                    purpose = State.SelectFactor.Purpose.Confirmation,
                    excludeFactorSources = state.confirmationFactors.map { it.id }.toPersistentList()
                )
            )
        }
    }

    fun onRemoveConfirmRecoveryFactor(card: FactorSourceCard) {
        viewModelScope.launch {
            securityShieldBuilderClient.removeFactorSourceFromConfirmation(card.id)
            initSelection()
        }
    }

    fun onFactorSelected(card: FactorSourceCard) {
        val selectFactor = _state.value.selectFactor ?: return

        viewModelScope.launch {
            when (selectFactor.purpose) {
                State.SelectFactor.Purpose.StartRecovery -> securityShieldBuilderClient.addFactorSourceToRecovery(card.id)
                State.SelectFactor.Purpose.Confirmation -> securityShieldBuilderClient.addFactorSourceToConfirmation(card.id)
            }
        }
    }

    fun onDismissSelectFactor() {
        _state.update { state -> state.copy(selectFactor = null) }
    }

    private fun initSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.recoveryRoleSelection()
                .collect { selection ->
                    _state.update { state ->
                        state.copy(
                            startRecoveryFactors = selection.startRecoveryFactors.map {
                                it.toFactorSourceCard(includeDescription = true, includeLastUsedOn = false)
                            }.toPersistentList(),
                            confirmationFactors = selection.confirmationFactors.map {
                                it.toFactorSourceCard(includeDescription = true, includeLastUsedOn = false)
                            }.toPersistentList(),
                            status = selection.shieldStatus,
                            fallbackPeriod = selection.timePeriodUntilAutoConfirm,
                            selectFallbackPeriod = null,
                            selectFactor = null
                        )
                    }
                }
        }
    }

    fun onFallbackPeriodClick() {
        _state.update { state ->
            state.copy(
                selectFallbackPeriod = State.SelectFallbackPeriod(
                    currentValue = requireNotNull(state.fallbackPeriod).value.toInt(),
                    currentUnit = state.fallbackPeriod.unit,
                    values = state.fallbackPeriod.unit.values.toPersistentList(),
                    units = TimePeriodUnit.entries.toPersistentList()
                )
            )
        }
    }

    fun onSetFallbackPeriodClick() {
        viewModelScope.launch {
            val fallbackPeriod = requireNotNull(state.value.selectFallbackPeriod)

            securityShieldBuilderClient.setTimePeriodUntilAutoConfirm(
                TimePeriod(
                    value = fallbackPeriod.currentValue.toUShort(),
                    unit = fallbackPeriod.currentUnit
                )
            )
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

    fun onFallbackPeriodUnitChange(unit: TimePeriodUnit) {
        _state.update { state ->
            val newValues = unit.values
            state.copy(
                selectFallbackPeriod = state.selectFallbackPeriod?.copy(
                    currentUnit = unit,
                    values = newValues.toPersistentList(),
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
        val status: SecurityShieldBuilderStatus? = null,
        val startRecoveryFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val confirmationFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val fallbackPeriod: TimePeriod? = null,
        val selectFallbackPeriod: SelectFallbackPeriod? = null,
        val selectFactor: SelectFactor? = null
    ) : UiState {

        val isButtonEnabled = status !is SecurityShieldBuilderStatus.Invalid

        data class SelectFactor(
            val purpose: Purpose,
            val excludeFactorSources: PersistentList<FactorSourceId>
        ) {

            enum class Purpose {
                StartRecovery,
                Confirmation
            }
        }

        data class SelectFallbackPeriod(
            val currentValue: Int,
            val currentUnit: TimePeriodUnit,
            val values: PersistentList<Int>,
            val units: PersistentList<TimePeriodUnit>
        )
    }

    sealed interface Event : OneOffEvent {

        data object ToNameSetup : Event
    }
}
