package com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.DepositGuaranteesViewModel.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.defaultDepositGuarantee
import rdx.works.profile.domain.depositguarantees.ChangeDefaultDepositGuaranteeUseCase
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DepositGuaranteesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeDefaultDepositGuaranteeUseCase: ChangeDefaultDepositGuaranteeUseCase,
) : StateViewModel<State>() {

    override fun initialState() = State()

    init {
        viewModelScope.launch {
            updateDepositGuarantee(depositGuarantee = getProfileUseCase.defaultDepositGuarantee().toBigDecimal())
        }
    }

    fun onDepositGuaranteeChanged(depositGuarantee: String) {
        if (depositGuarantee.contains("-")) {
            return
        }

        val updatedDepositGuarantee = depositGuarantee.toBigDecimalOrNull()?.divide(HUNDRED)

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
                updatedDepositGuarantee = getProfileUseCase.defaultDepositGuarantee().toBigDecimal().add(DEPOSIT_CHANGE_THRESHOLD)
            )
        }
    }

    fun onDepositGuaranteeDecreased() {
        viewModelScope.launch {
            val updatedDepositGuarantee = getProfileUseCase.defaultDepositGuarantee().toBigDecimal()
                .subtract(DEPOSIT_CHANGE_THRESHOLD).coerceAtLeast(BigDecimal.ZERO)
            changeAndUpdateDepositGuarantee(
                updatedDepositGuarantee = updatedDepositGuarantee
            )
        }
    }

    private suspend fun changeAndUpdateDepositGuarantee(updatedDepositGuarantee: BigDecimal?) {
        updateDepositGuarantee(depositGuarantee = updatedDepositGuarantee)
        updatedDepositGuarantee?.let { depositGuarantee ->
            changeDefaultDepositGuaranteeUseCase.invoke(defaultDepositGuarantee = depositGuarantee.toDouble())
        }
    }

    private fun updateDepositGuarantee(depositGuarantee: BigDecimal?) {
        if (depositGuarantee != null) {
            _state.update { state ->
                state.copy(
                    isDepositInputValid = true,
                    depositGuarantee = depositGuarantee.multiply(HUNDRED).stripTrailingZeros().toPlainString()
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
        private val DEPOSIT_CHANGE_THRESHOLD = BigDecimal("0.01")
        private val HUNDRED = BigDecimal("100")
    }
}
