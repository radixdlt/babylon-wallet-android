package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.data.repository.securityshield.model.ChooseFactorSourceContext
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common.notEnoughFactors
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorListKind
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
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
    private val shieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<SetupRegularAccessViewModel.State>() {

    init {
        observeSelection()
    }

    override fun initialState(): State = State()

    fun onThresholdClick() {
        viewModelScope.launch {
            val selection = shieldBuilderClient.primaryRoleSelection().first()
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
        viewModelScope.launch { shieldBuilderClient.executeMutatingFunction { setThreshold(threshold) } }
    }

    fun onThresholdSelectionDismiss() {
        _state.update { it.copy(selectThreshold = null) }
    }

    fun onAddThresholdFactorClick() {
        onAddFactorClick(ChooseFactorSourceContext.PrimaryThreshold)
    }

    fun onRemoveThresholdFactorClick(card: FactorSourceCard) {
        viewModelScope.launch {
            shieldBuilderClient.executeMutatingFunction { removeFactorFromPrimary(card.id, FactorListKind.THRESHOLD) }
        }
    }

    fun onAddOverrideClick() {
        _state.update { state ->
            state.copy(isOverrideSectionVisible = true)
        }
    }

    fun onAddOverrideFactorClick() {
        onAddFactorClick(ChooseFactorSourceContext.PrimaryOverride)
    }

    fun onAddAuthenticationFactorClick() {
        onAddFactorClick(ChooseFactorSourceContext.AuthenticationSigning)
    }

    fun onFactorSelected(card: FactorSourceCard) {
        val selectFactor = _state.value.selectFactor ?: return

        viewModelScope.launch {
            shieldBuilderClient.executeMutatingFunction {
                when (selectFactor.context) {
                    ChooseFactorSourceContext.PrimaryThreshold -> addFactorSourceToPrimaryThreshold(card.id)
                    ChooseFactorSourceContext.PrimaryOverride -> addFactorSourceToPrimaryOverride(card.id)
                    ChooseFactorSourceContext.AuthenticationSigning -> setAuthenticationSigningFactor(card.id)
                    else -> error("Regular Access cannot have this context: ${selectFactor.context}")
                }
            }
        }
    }

    fun onDismissSelectFactor() {
        _state.update { state -> state.copy(selectFactor = null) }
    }

    fun onRemoveAuthenticationFactorClick() {
        viewModelScope.launch { shieldBuilderClient.executeMutatingFunction { setAuthenticationSigningFactor(null) } }
    }

    fun onRemoveOverrideFactorClick(card: FactorSourceCard) {
        viewModelScope.launch {
            shieldBuilderClient.executeMutatingFunction { removeFactorFromPrimary(card.id, FactorListKind.OVERRIDE) }
        }
    }

    fun onRemoveAllOverrideFactorsClick() {
        viewModelScope.launch { shieldBuilderClient.executeMutatingFunction { removeAllFactorsFromPrimaryOverride() } }
        _state.update { state -> state.copy(isOverrideSectionVisible = false) }
    }

    private fun observeSelection() {
        viewModelScope.launch {
            shieldBuilderClient.primaryRoleSelection()
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

    private fun onAddFactorClick(context: ChooseFactorSourceContext) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    selectFactor = State.SelectFactor(
                        context = context,
                        alreadySelectedFactorSources = shieldBuilderClient.findAlreadySelectedFactorSourceIds(context).toPersistentList(),
                        unusableFactorSourceKinds = shieldBuilderClient.getUnusableFactorSourceKinds(context).toPersistentList()
                    )
                )
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
        val isOverrideSectionVisible: Boolean = false,
        val message: UiMessage? = null,
        val selectFactor: SelectFactor? = null
    ) : UiState {

        private val invalidStatus = status as? SecurityShieldBuilderStatus.Invalid

        val factorListStatus = when {
            status is SecurityShieldBuilderStatus.Weak -> FactorListStatus.Unsafe
            invalidStatus?.reason?.isPrimaryRoleFactorListEmpty == true -> FactorListStatus.PrimaryEmpty
            invalidStatus?.reason.notEnoughFactors() -> FactorListStatus.NotEnoughFactors
            else -> FactorListStatus.Ok
        }
        val isAuthSigningFactorMissing: Boolean = invalidStatus?.reason?.isAuthSigningFactorMissing == true

        val isButtonEnabled = factorListStatus != FactorListStatus.PrimaryEmpty && !isAuthSigningFactorMissing

        enum class FactorListStatus {
            PrimaryEmpty,
            NotEnoughFactors,
            Unsafe,
            Ok
        }

        data class SelectFactor(
            val context: ChooseFactorSourceContext,
            val alreadySelectedFactorSources: PersistentList<FactorSourceId>,
            val unusableFactorSourceKinds: PersistentList<FactorSourceKind>
        )

        data class SelectThreshold(
            val current: Threshold,
            val items: PersistentList<Threshold>
        )
    }
}
