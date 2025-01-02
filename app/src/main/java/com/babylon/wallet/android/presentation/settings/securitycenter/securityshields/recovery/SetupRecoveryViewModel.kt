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
                            status = selection.shieldStatus
                        )
                    }
                }
        }
    }

    data class State(
        val status: SecurityShieldBuilderInvalidReason? = null,
        val startFactors: PersistentList<FactorSourceCard> = persistentListOf(),
        val confirmFactors: PersistentList<FactorSourceCard> = persistentListOf(),
    ) : UiState {

        val isButtonEnabled = status == null
    }
}
