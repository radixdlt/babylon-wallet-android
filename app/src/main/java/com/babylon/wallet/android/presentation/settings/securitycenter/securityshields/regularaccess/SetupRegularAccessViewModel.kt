package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorListKind
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.Threshold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
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

    fun onThresholdClick() {
        viewModelScope.launch {
            val selection = securityShieldBuilderClient.primaryRoleSelection().first()
            if (selection.thresholdValues.isEmpty()) {
                return@launch
            }

            _state.update {
                it.copy(
                    selectThreshold = State.SelectThreshold(
                        current = if (selection.threshold in selection.thresholdValues) {
                            selection.threshold
                        } else {
                            selection.thresholdValues.first()
                        },
                        items = selection.thresholdValues.toPersistentList()
                    )
                )
            }
        }
    }

    fun onThresholdSelect(threshold: Threshold) {
        viewModelScope.launch { securityShieldBuilderClient.executeMutatingFunction { setThreshold(threshold) } }
    }

    fun onThresholdSelectionDismiss() {
        _state.update { it.copy(selectThreshold = null) }
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
        viewModelScope.launch {
            securityShieldBuilderClient.executeMutatingFunction {
                removeFactorFromPrimary(
                    card.id,
                    FactorListKind.THRESHOLD
                )
            }
        }
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
            securityShieldBuilderClient.executeMutatingFunction {
                when (selectFactor.purpose) {
                    State.SelectFactor.Purpose.Threshold -> addFactorSourceToPrimaryThreshold(card.id)
                    State.SelectFactor.Purpose.Override -> addFactorSourceToPrimaryOverride(card.id)
                    State.SelectFactor.Purpose.Authentication -> setAuthenticationSigningFactor(card.id)
                }
            }
        }
    }

    fun onDismissSelectFactor() {
        _state.update { state -> state.copy(selectFactor = null) }
    }

    fun onRemoveAuthenticationFactorClick() {
        viewModelScope.launch { securityShieldBuilderClient.executeMutatingFunction { setAuthenticationSigningFactor(null) } }
    }

    fun onRemoveOverrideFactorClick(card: FactorSourceCard) {
        viewModelScope.launch {
            securityShieldBuilderClient.executeMutatingFunction {
                removeFactorFromPrimary(card.id, FactorListKind.OVERRIDE)
            }
        }
    }

    fun onRemoveAllOverrideFactorsClick() {
        viewModelScope.launch { securityShieldBuilderClient.executeMutatingFunction { removeAllFactorsFromPrimaryOverride() } }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            securityShieldBuilderClient.primaryRoleSelection()
                .collect { selection ->
                    _state.update { state ->
                        state.copy(
                            thresholdFactors = selection.thresholdFactors.map {
                                it.toFactorSourceCard(
                                    includeDescription = true,
                                    includeLastUsedOn = false
                                )
                            }.toPersistentList(),
                            overrideFactors = selection.overrideFactors.map {
                                it.toFactorSourceCard(
                                    includeDescription = true,
                                    includeLastUsedOn = false
                                )
                            }.toPersistentList(),
                            authenticationFactor = selection.authenticationFactor?.toFactorSourceCard(
                                includeDescription = true,
                                includeLastUsedOn = false
                            ),
                            status = selection.shieldStatus,
                            threshold = selection.threshold,
                            selectThreshold = null,
                            selectFactor = null
                        )
                    }
                }
        }
    }

    data class State(
        private val status: SecurityShieldBuilderStatus? = null,
        val threshold: Threshold = Threshold.All,
        val selectThreshold: SelectThreshold? = null,
        val thresholdFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val overrideFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val authenticationFactor: FactorSourceCard? = null,
        val message: UiMessage? = null,
        val selectFactor: SelectFactor? = null
    ) : UiState {

        private val invalidStatus = status as? SecurityShieldBuilderStatus.Invalid

        val factorListStatus = when {
            status is SecurityShieldBuilderStatus.Weak -> FactorListStatus.Unsafe
            invalidStatus?.reason?.isPrimaryRoleFactorListEmpty == true -> FactorListStatus.Empty
            else -> FactorListStatus.Ok
        }
        val isAuthSigningFactorMissing: Boolean = invalidStatus?.reason?.isAuthSigningFactorMissing == true

        val isButtonEnabled = factorListStatus != FactorListStatus.Empty && !isAuthSigningFactorMissing

        enum class FactorListStatus {
            Empty,
            Unsafe,
            Ok
        }

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

        data class SelectThreshold(
            val current: Threshold,
            val items: PersistentList<Threshold>
        )
    }
}
