package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.isZero
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class TransactionFeesDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private var searchFeePayersJob: Job? = null

    @Suppress("NestedBlockDepth")
    suspend fun onCustomizeClick() {
        if (_state.value.transactionFees.defaultTransactionFee.isZero) {
            // None required
            _state.update { state ->
                state.noneRequiredState()
            }
        } else {
            _state.value.feePayers?.let { feePayerResult ->
                if (feePayerResult.selectedAccountAddress != null) {
                    // Candidate selected
                    getProfileUseCase().activeAccountOnCurrentNetwork(withAddress = feePayerResult.selectedAccountAddress)
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
        val newTransactionFees = transactionFees.copy(
            feePaddingAmount = feePaddingAmount
        )
        _state.update { state ->
            state.copy(
                transactionFees = newTransactionFees
            )
        }

        searchFeePayersJob?.cancel()
        searchFeePayersJob = viewModelScope.launch {
            val feePayers = _state.value.feePayers ?: return@launch
            val newFeePayers = feePayers.copy(
                candidates = feePayers.candidates.map {
                    it.copy(
                        hasEnoughBalance = it.xrdAmount >= newTransactionFees.transactionFeeToLock
                    )
                }
            )

            _state.update { state ->
                state.copy(
                    feePayers = newFeePayers
                )
            }
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
