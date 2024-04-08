package com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.DepositGuaranteesViewModel.State
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.formatted
import rdx.works.core.domain.toDecimal192OrNull
import rdx.works.core.domain.toDouble
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.defaultDepositGuarantee
import rdx.works.profile.domain.depositguarantees.ChangeDefaultDepositGuaranteeUseCase
import javax.inject.Inject

@HiltViewModel
class DepositGuaranteesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeDefaultDepositGuaranteeUseCase: ChangeDefaultDepositGuaranteeUseCase,
) : StateViewModel<State>() {

    override fun initialState() = State()

    init {
        viewModelScope.launch {
            updateDepositGuarantee(depositGuarantee = getProfileUseCase.defaultDepositGuarantee().toDecimal192())
        }
    }

    fun onDepositGuaranteeChanged(depositGuarantee: String) {
        if (depositGuarantee.contains("-")) {
            return
        }

        val updatedDepositGuarantee = depositGuarantee.toDecimal192OrNull()?.let {
            it / HUNDRED
        }

        _state.update { state ->
            state.copy(
                isDepositInputValid = updatedDepositGuarantee != null,
                depositGuarantee = depositGuarantee
            )
        }

        viewModelScope.launch {
            updatedDepositGuarantee?.let { depositGuarantee ->
                changeDefaultDepositGuaranteeUseCase.invoke(defaultDepositGuarantee = depositGuarantee.toDouble())
            }
        }
    }

    fun onDepositGuaranteeIncreased() {
        viewModelScope.launch {
            changeAndUpdateDepositGuarantee(
                updatedDepositGuarantee = getProfileUseCase.defaultDepositGuarantee().toDecimal192() + DEPOSIT_CHANGE_THRESHOLD
            )
        }
    }

    fun onDepositGuaranteeDecreased() {
        viewModelScope.launch {
            val updatedDepositGuarantee = (getProfileUseCase.defaultDepositGuarantee().toDecimal192() - DEPOSIT_CHANGE_THRESHOLD).clamped
            changeAndUpdateDepositGuarantee(
                updatedDepositGuarantee = updatedDepositGuarantee
            )
        }
    }

    private suspend fun changeAndUpdateDepositGuarantee(updatedDepositGuarantee: Decimal192?) {
        updateDepositGuarantee(depositGuarantee = updatedDepositGuarantee)
        updatedDepositGuarantee?.let { depositGuarantee ->
            changeDefaultDepositGuaranteeUseCase.invoke(defaultDepositGuarantee = depositGuarantee.toDouble())
        }
    }

    private fun updateDepositGuarantee(depositGuarantee: Decimal192?) {
        if (depositGuarantee != null) {
            _state.update { state ->
                state.copy(
                    isDepositInputValid = true,
                    depositGuarantee = (depositGuarantee * HUNDRED).formatted()
                )
            }
        } else {
            _state.update { state ->
                state.copy(
                    isDepositInputValid = false
                )
            }
        }
    }

    data class State(
        val isDepositInputValid: Boolean = true,
        val depositGuarantee: String? = null
    ) : UiState

    companion object {
        private val DEPOSIT_CHANGE_THRESHOLD = 0.01.toDecimal192()
        private val HUNDRED = 100.toDecimal192()
    }
}
