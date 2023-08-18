package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
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
        val mostRecentFeesMode = state.value.feesMode

        if (state.value.transactionFees.defaultTransactionFee == BigDecimal.ZERO) {
            // None required
            state.update { state ->
                state.copy(
                    feesMode = mostRecentFeesMode,
                    sheetState = State.Sheet.CustomizeFees(
                        feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired
                    )
                )
            }
        } else {
            state.value.feePayerSearchResult?.let { feePayerResult ->
                if (feePayerResult.feePayerAddressFromManifest != null) {
                    // Candidate selected
                    getProfileUseCase.accountOnCurrentNetwork(withAddress = feePayerResult.feePayerAddressFromManifest)
                        ?.let { feePayerCandidate ->
                            state.update { state ->
                                state.copy(
                                    feesMode = mostRecentFeesMode,
                                    sheetState = State.Sheet.CustomizeFees(
                                        feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                                            feePayerCandidate = feePayerCandidate
                                        )
                                    )
                                )
                            }
                        }
                } else {
                    // No candidate selected
                    state.update { state ->
                        state.copy(
                            feesMode = mostRecentFeesMode,
                            sheetState = State.Sheet.CustomizeFees(
                                feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected(
                                    candidates = feePayerResult.candidates
                                )
                            )
                        )
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
                    tipPercentage = tipPercentage.toBigDecimalOrNull()
                )
            )
        }
    }

    fun onViewDefaultModeClick() {
        val transactionFees = state.value.transactionFees
        state.update {
            it.copy(
                feesMode = State.Sheet.CustomizeFees.FeesMode.Default,
                // When switching back to default mode, reset field values that have been modified in advanced mode
                transactionFees = transactionFees.copy(
                    feePaddingAmount = null,
                    tipPercentage = null
                )
            )
        }
    }

    fun onViewAdvancedModeClick() {
        state.update {
            it.copy(
                feesMode = State.Sheet.CustomizeFees.FeesMode.Advanced,
            )
        }
    }

    private fun switchToFeePayerSelection() {
        val feePayerResult = state.value.feePayerSearchResult
        val transactionFees = state.value.transactionFees
        state.update { state ->
            state.copy(
                transactionFees = transactionFees,
                sheetState = State.Sheet.CustomizeFees(
                    feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.SelectFeePayer(
                        candidates = feePayerResult?.candidates.orEmpty()
                    )
                )
            )
        }
    }
}
