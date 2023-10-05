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
import rdx.works.profile.domain.preferences.ChangeDefaultDepositGuaranteeUseCase
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class DepositGuaranteesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeDefaultDepositGuaranteeUseCase: ChangeDefaultDepositGuaranteeUseCase,
) : StateViewModel<State>() {

    override fun initialState() = State()

    init {
        viewModelScope.launch {
            updateDepositGuarantee(depositGuarantee = getProfileUseCase.defaultDepositGuarantee())
        }
    }

    fun onDepositGuaranteeChanged(depositGuarantee: String) {
        val updatedDepositGuarantee = depositGuarantee.toDoubleOrNull()?.div(HUNDRED)

        _state.update { state ->
            state.copy(
                isDepositInputValid = updatedDepositGuarantee != null,
                depositGuarantee = depositGuarantee,
                depositGuaranteeDouble = updatedDepositGuarantee
            )
        }

        viewModelScope.launch {
            updatedDepositGuarantee?.let { depositGuarantee ->
                changeDefaultDepositGuaranteeUseCase.invoke(defaultDepositGuarantee = depositGuarantee)
            }
        }
    }

    fun onDepositGuaranteeIncreased() {
        viewModelScope.launch {
            changeAndUpdateDepositGuarantee(
                updatedDepositGuarantee = getProfileUseCase.defaultDepositGuarantee().plus(DEPOSIT_CHANGE_THRESHOLD)
            )
        }
    }

    fun onDepositGuaranteeDecreased() {
        viewModelScope.launch {
            changeAndUpdateDepositGuarantee(
                updatedDepositGuarantee = getProfileUseCase.defaultDepositGuarantee().minus(DEPOSIT_CHANGE_THRESHOLD)
            )
        }
    }

    private suspend fun changeAndUpdateDepositGuarantee(updatedDepositGuarantee: Double?) {
        updateDepositGuarantee(depositGuarantee = updatedDepositGuarantee)
        updatedDepositGuarantee?.let { depositGuarantee ->
            changeDefaultDepositGuaranteeUseCase.invoke(defaultDepositGuarantee = depositGuarantee)
        }
    }

    private fun updateDepositGuarantee(depositGuarantee: Double?) {
        if (depositGuarantee != null) {
            _state.update { state ->
                state.copy(
                    isDepositInputValid = true,
                    depositGuarantee = DecimalFormat("0.#").format(depositGuarantee.times(HUNDRED)),
                    depositGuaranteeDouble = depositGuarantee
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
        val depositGuarantee: String? = null,
        val depositGuaranteeDouble: Double? = null
    ) : UiState

    companion object {
        private const val DEPOSIT_CHANGE_THRESHOLD = 0.001
        private const val HUNDRED = 100
    }
}
