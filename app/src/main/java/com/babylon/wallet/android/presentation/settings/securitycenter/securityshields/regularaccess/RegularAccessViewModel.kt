package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegularAccessViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<RegularAccessViewModel.State>() {

    override fun initialState(): State = State()

    fun onNumberOfFactorsClick() {
        TODO("Not yet implemented")
    }

    fun onAddFactorClick() {
        TODO("Not yet implemented")
    }

    fun onRemoveFactorClick(card: FactorSourceInstanceCard) {
        TODO("Not yet implemented")
    }

    fun onAddOverrideClick() {
        TODO("Not yet implemented")
    }

    fun onAddLoginFactorClick() {
        TODO("Not yet implemented")
    }

    fun onRemoveLoginFactorClick() {
        TODO("Not yet implemented")
    }

    fun onRemoveOverrideFactorClick(factorSourceInstanceCard: FactorSourceInstanceCard) {
        TODO("Not yet implemented")
    }

    fun onRemoveAllOverrideFactorsClick() {
        TODO("Not yet implemented")
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