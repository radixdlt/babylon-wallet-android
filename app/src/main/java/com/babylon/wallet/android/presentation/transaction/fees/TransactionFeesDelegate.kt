package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.math.BigDecimal

class TransactionFeesDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase
) {

    @Suppress("NestedBlockDepth")
    suspend fun onCustomizeClick() {
        if (state.value.transactionFees.defaultTransactionFee == BigDecimal.ZERO) {
            // None required
            state.update { state ->
                state.noneRequiredState()
            }
        } else {
            state.value.feePayerSearchResult?.let { feePayerResult ->
                if (feePayerResult.feePayerAddressFromManifest != null) {
                    // Candidate selected
                    getProfileUseCase.accountOnCurrentNetwork(withAddress = feePayerResult.feePayerAddressFromManifest)
                        ?.let { feePayerCandidate ->
                            state.update { state ->
                                state.candidateSelectedState(feePayerCandidate)
                            }
                        }
                } else {
                    // No candidate selected
                    state.update { state ->
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
        val transactionFees = state.value.transactionFees
        state.update { state ->
            state.copy(
                transactionFees = transactionFees.copy(
                    feePaddingAmount = feePaddingAmount
                )
            )
        }
    }

    @Suppress("MagicNumber")
    fun onTipPercentageChanged(tipPercentage: String) {
        val transactionFees = state.value.transactionFees

        state.update { state ->
            state.copy(
                transactionFees = transactionFees.copy(
                    tipPercentage = tipPercentage
                )
            )
        }
    }

    fun onViewDefaultModeClick() {
        state.update { state ->
            state.defaultModeState()
        }
    }

    fun onViewAdvancedModeClick() {
        state.update { state ->
            state.advancedModeState()
        }
    }

    private fun switchToFeePayerSelection() {
        state.update { state ->
            state.feePayerSelectionState()
        }
    }
}
