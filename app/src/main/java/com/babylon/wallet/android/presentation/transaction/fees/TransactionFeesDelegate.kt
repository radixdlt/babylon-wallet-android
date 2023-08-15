package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.lang.NumberFormatException
import java.math.BigDecimal

class TransactionFeesDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase
) {

    @Suppress("NestedBlockDepth")
    suspend fun onCustomizeClick() {
        val hasNetworkOrTipBeenSetup = state.value.transactionFees.hasNetworkOrTipBeenSetup
        val feesMode = if (hasNetworkOrTipBeenSetup) {
            State.Sheet.CustomizeFees.FeesMode.Advanced
        } else {
            State.Sheet.CustomizeFees.FeesMode.Default
        }

        if (state.value.transactionFees.defaultTransactionFee == BigDecimal.ZERO) {
            // None required
            state.update { state ->
                state.copy(
                    sheetState = State.Sheet.CustomizeFees(
                        feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                        feesMode = feesMode
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
                                    sheetState = State.Sheet.CustomizeFees(
                                        feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                                            feePayerCandidate = feePayerCandidate
                                        ),
                                        feesMode = feesMode
                                    )
                                )
                            }
                        }
                } else {
                    // No candidate selected
                    state.update { state ->
                        state.copy(
                            sheetState = State.Sheet.CustomizeFees(
                                feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected(
                                    candidates = feePayerResult.candidates
                                ),
                                feesMode = feesMode
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

    fun onNetworkAndRoyaltyFeeChanged(networkAndRoyaltyFee: String) {
        val transactionFees = state.value.transactionFees
        state.update { state ->
            state.copy(
                transactionFees = transactionFees.copy(
                    networkAndRoyaltyFees = networkAndRoyaltyFee
                )
            )
        }
    }

    @Suppress("MagicNumber")
    fun onTipPercentageChanged(tipPercentage: String) {
        try {
            if (tipPercentage.toBigDecimal() > BigDecimal(100) || tipPercentage.contains(".")) {
                return
            }
        } catch (_: NumberFormatException) { }

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
        val customizeFeesSheet = state.value.sheetState as? State.Sheet.CustomizeFees ?: return
        val transactionFees = state.value.transactionFees
        state.update {
            it.copy(
                // When switching back to default mode, reset field values that have been modified in advanced mode
                transactionFees = transactionFees.copy(
                    networkAndRoyaltyFees = null,
                    tipPercentage = null
                ),
                sheetState = customizeFeesSheet.copy(
                    feesMode = State.Sheet.CustomizeFees.FeesMode.Default
                )
            )
        }
    }

    fun onViewAdvancedModeClick() {
        val customizeFeesSheet = state.value.sheetState as? State.Sheet.CustomizeFees ?: return
        state.update {
            it.copy(
                sheetState = customizeFeesSheet.copy(
                    feesMode = State.Sheet.CustomizeFees.FeesMode.Advanced
                )
            )
        }
    }

    private fun switchToFeePayerSelection() {
        val feePayerResult = state.value.feePayerSearchResult
        val feesMode = state.value.sheetState as State.Sheet.CustomizeFees ?: return
        val transactionFees = state.value.transactionFees
        state.update { state ->
            state.copy(
                transactionFees = transactionFees,
                sheetState = State.Sheet.CustomizeFees(
                    feePayerMode = State.Sheet.CustomizeFees.FeePayerMode.SelectFeePayer(
                        candidates = feePayerResult?.candidates.orEmpty()
                    ),
                    feesMode = feesMode.feesMode
                )
            )
        }
    }
}
