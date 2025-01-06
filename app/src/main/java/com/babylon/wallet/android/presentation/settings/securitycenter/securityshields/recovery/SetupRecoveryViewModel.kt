package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
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
import timber.log.Timber
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

    fun onContinueClick() {
        _state.update { state ->
            state.copy(
                setShieldName = State.SetShieldName(name = "")
            )
        }
    }

    private fun initSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.recoveryRoleSelection()
                .collect { selection ->
                    _state.update { state ->
                        state.copy(
                            startRecoveryFactors = selection.startRecoveryFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                            confirmationFactors = selection.confirmationFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
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

    fun onShieldNameChange(value: String) {
        _state.update { state ->
            state.copy(
                setShieldName = state.setShieldName?.copy(name = value)
            )
        }
    }

    fun onDismissSetShieldName() {
        _state.update { state -> state.copy(setShieldName = null) }
    }

    fun onConfirmShieldNameClick() {
        viewModelScope.launch {
            val shieldName = requireNotNull(state.value.setShieldName?.name)
            val securityStructureOfFactorSourceIDs = securityShieldBuilderClient.buildShield(shieldName)

            // TODO save shield to profile
            Timber.w("Security structure: $securityStructureOfFactorSourceIDs")

            sendEvent(Event.ShieldCreated)
        }
    }

    data class State(
        val status: SecurityShieldBuilderInvalidReason? = null,
        val startRecoveryFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val confirmationFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val fallbackPeriod: FallbackPeriod? = null,
        val selectFallbackPeriod: SelectFallbackPeriod? = null,
        val selectFactor: SelectFactor? = null,
        val setShieldName: SetShieldName? = null
    ) : UiState {

        val isButtonEnabled = status == null

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

        data class SetShieldName(
            val name: String
        ) {

            val isButtonEnabled = name.isNotBlank()
        }
    }

    sealed interface Event : OneOffEvent {

        data object ShieldCreated : Event
    }

    companion object {

        private const val DAYS_IN_A_WEEK = 7
        private val FALLBACK_PERIOD_DAYS = (1 until 15).toPersistentList()
        private val FALLBACK_PERIOD_WEEKS = (1 until 5).toPersistentList()
    }
}
