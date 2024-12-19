package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common.toCompactInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegularAccessViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<RegularAccessViewModel.State>() {

    init {
        viewModelScope.launch {
            val selection = securityShieldBuilderClient.getPrimaryRoleSelection()

            _state.update { state ->
                state.copy(
                    thresholdFactors = selection.thresholdFactors.map { it.toCompactInstanceCard(true) },
                    overrideFactors = selection.overrideFactors.map { it.toCompactInstanceCard(true) },
                    loginFactor = selection.loginFactor?.toCompactInstanceCard(true)
                )
            }
        }
    }

    override fun initialState(): State = State()

    fun onNumberOfFactorsClick() {
    }

    fun onAddThresholdFactorClick() {
    }

    fun onRemoveThresholdFactorClick(card: FactorSourceInstanceCard) {
    }

    fun onAddOverrideClick() {
    }

    fun onAddLoginFactorClick() {
    }

    fun onRemoveLoginFactorClick() {
    }

    fun onRemoveOverrideFactorClick(factorSourceInstanceCard: FactorSourceInstanceCard) {
    }

    fun onRemoveAllOverrideFactorsClick() {
    }

    data class State(
        val status: SelectedFactorSourcesForRoleStatus? = null,
        val numberOfFactors: NumberOfFactors = NumberOfFactors.All,
        val thresholdFactors: List<FactorSourceInstanceCard> = emptyList(),
        val overrideFactors: List<FactorSourceInstanceCard> = emptyList(),
        val loginFactor: FactorSourceInstanceCard? = null,
        val isButtonEnabled: Boolean = false,
        val message: UiMessage? = null
    ) : UiState {

        sealed interface NumberOfFactors {

            data object All : NumberOfFactors

            data class Count(
                val value: Int
            ) : NumberOfFactors
        }
    }
}