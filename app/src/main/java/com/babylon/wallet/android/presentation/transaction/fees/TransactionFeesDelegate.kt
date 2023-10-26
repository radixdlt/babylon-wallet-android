package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.coroutines.flow.update
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.math.BigDecimal
import javax.inject.Inject

class TransactionFeesDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    @Suppress("NestedBlockDepth")
    suspend fun onCustomizeClick() {
        if (_state.value.transactionFees.defaultTransactionFee == BigDecimal.ZERO) {
            // None required
            _state.update { state ->
                state.noneRequiredState()
            }
        } else {
            _state.value.feePayerSearchResult?.let { feePayerResult ->
                if (feePayerResult.feePayerAddress != null) {
                    // Candidate selected
                    getProfileUseCase.accountOnCurrentNetwork(withAddress = feePayerResult.feePayerAddress)
                        ?.let { feePayerCandidate ->
                            _state.update { state ->
                                state.candidateSelectedState(feePayerCandidate)
                            }
                        }
                } else {
                    // No candidate selected
                    _state.update { state ->
                        state.noCandidateSelectedState()
                    }
                }
            }
        }
    }

    fun onChangeFeePayerClick() {
        switchToFeePayerSelection()
    }

    fun onSelectFeePayerClick() {
        switchToFeePayerSelection()
    }

    fun onFeePaddingAmountChanged(feePaddingAmount: String) {
        val transactionFees = _state.value.transactionFees
        _state.update { state ->
            state.copy(
                transactionFees = transactionFees.copy(
                    feePaddingAmount = feePaddingAmount
                )
            )
        }
    }

    @Suppress("MagicNumber")
    fun onTipPercentageChanged(tipPercentage: String) {
        val transactionFees = _state.value.transactionFees

        _state.update { state ->
            state.copy(
                transactionFees = transactionFees.copy(
                    tipPercentage = tipPercentage.filter { it.isDigit() }
                )
            )
        }
    }

    fun onViewDefaultModeClick() {
        _state.update { state ->
            state.defaultModeState()
        }
    }

    fun onViewAdvancedModeClick() {
        _state.update { state ->
            state.advancedModeState()
        }
    }

    private fun switchToFeePayerSelection() {
        _state.update { state ->
            state.feePayerSelectionState()
        }
    }
}
