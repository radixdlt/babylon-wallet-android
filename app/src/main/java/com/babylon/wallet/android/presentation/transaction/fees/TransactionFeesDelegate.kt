package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.analysis.FeesResolver
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class TransactionFeesDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionFees")

    private var searchFeePayersJob: Job? = null

    suspend fun resolveFees() {
        val executionSummary = data.value.transactionToReviewData.transactionToReview.executionSummary

        val transactionFees = FeesResolver.resolve(
            summary = executionSummary,
            notaryAndSigners = data.value.notaryAndSigners,
            previewType = _state.value.previewType
        )

        searchFeePayersUseCase(
            feePayerCandidates = data.value.feePayerCandidates,
            lockFee = transactionFees.defaultTransactionFee
        ).onSuccess { feePayers ->
            _state.update { it.copy(isNetworkFeeLoading = false) }
            updateFeePayers(feePayers)
            fetchXrdPrice()
        }.onFailure { throwable ->
            logger.w(throwable)

            _state.update {
                it.copy(
                    isLoading = false,
                    isNetworkFeeLoading = false,
                    previewType = PreviewType.None,
                    error = TransactionErrorMessage(throwable)
                )
            }
        }
    }

    suspend fun onCustomizeClick() {
        val transactionFeesInfo = _state.value.transactionFeesInfo ?: return

        if (transactionFeesInfo.transactionFees.defaultTransactionFee.isZero) {
            // None required
            _state.update { state ->
                state.copy(
                    sheetState = Sheet.CustomizeFees(
                        feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                        feesMode = data.value.latestFeesMode,
                        feesInfo = transactionFeesInfo
                    )
                )
            }
        } else {
            val feePayers = data.value.feePayers ?: return
            if (feePayers.selectedAccountAddress != null) {
                // Candidate selected
                val feePayerCandidate = getProfileUseCase().activeAccountOnCurrentNetwork(
                    withAddress = feePayers.selectedAccountAddress
                ) ?: return
                _state.update { state ->
                    state.copy(
                        sheetState = Sheet.CustomizeFees(
                            feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                                feePayerCandidate = feePayerCandidate
                            ),
                            feesMode = data.value.latestFeesMode,
                            feesInfo = transactionFeesInfo
                        )
                    )
                }
            } else {
                // No candidate selected
                _state.update { state ->
                    state.copy(
                        sheetState = Sheet.CustomizeFees(
                            feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerSelected(
                                candidates = data.value.feePayers?.candidates.orEmpty()
                            ),
                            feesMode = data.value.latestFeesMode,
                            feesInfo = transactionFeesInfo
                        )
                    )
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

    fun onFeePayerChanged(selectedFeePayer: TransactionFeePayers.FeePayerCandidate) {
        val selectFeePayerInput = _state.value.selectedFeePayerInput ?: return

        _state.update {
            it.copy(
                selectedFeePayerInput = selectFeePayerInput.copy(
                    preselectedCandidate = selectedFeePayer
                )
            )
        }
    }

    fun onFeePayerSelected() {
        val selectFeePayerInput = _state.value.selectedFeePayerInput ?: return
        val selectedFeePayerAccount = selectFeePayerInput.preselectedCandidate?.account ?: return
        val feePayerSearchResult = data.value.feePayers ?: return

        val signersCount = data.value.notaryAndSigners.signers.count()

        val updatedFeePayerResult = feePayerSearchResult.copy(
            selectedAccountAddress = selectedFeePayerAccount.address,
            candidates = feePayerSearchResult.candidates
        )
        updateFeePayers(updatedFeePayerResult)

        val customizeFeesSheet = _state.value.sheetState as? Sheet.CustomizeFees ?: return
        val updatedSignersCount = if (isSelectedFeePayerInvolvedInTransaction(selectedFeePayerAccount.address)) {
            signersCount
        } else {
            signersCount + 1
        }

        _state.update {
            it.copy(
                transactionFeesInfo = it.transactionFeesInfo?.copy(
                    transactionFees = it.transactionFeesInfo.transactionFees.copy(
                        signersCount = updatedSignersCount
                    )
                ),
                sheetState = customizeFeesSheet.copy(
                    feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                        feePayerCandidate = selectedFeePayerAccount
                    )
                )
            )
        }
    }

    fun onFeePayerSelectionDismissRequest() {
        _state.update { it.copy(selectedFeePayerInput = null) }
    }

    fun onFeePaddingAmountChanged(feePaddingAmount: String) {
        val transactionFeesInfo = requireNotNull(_state.value.transactionFeesInfo)
        val newTransactionFees = transactionFeesInfo.transactionFees.copy(
            feePaddingAmount = feePaddingAmount
        )
        _state.update { state ->
            state.copy(
                transactionFeesInfo = transactionFeesInfo.copy(
                    transactionFees = newTransactionFees
                )
            )
        }

        searchFeePayersJob?.cancel()
        searchFeePayersJob = viewModelScope.launch {
            val feePayers = data.value.feePayers ?: return@launch
            val newFeePayers = feePayers.copy(
                candidates = feePayers.candidates.map {
                    it.copy(
                        hasEnoughBalance = it.xrdAmount >= newTransactionFees.transactionFeeToLock
                    )
                }
            )
            updateFeePayers(newFeePayers)
        }
    }

    @Suppress("MagicNumber")
    fun onTipPercentageChanged(tipPercentage: String) {
        _state.update { state ->
            state.copy(
                transactionFeesInfo = state.transactionFeesInfo?.copy(
                    transactionFees = state.transactionFeesInfo.transactionFees.copy(
                        tipPercentage = tipPercentage.filter { it.isDigit() }
                    )
                )
            )
        }
    }

    fun onViewDefaultModeClick() {
        data.update {
            it.copy(
                latestFeesMode = Sheet.CustomizeFees.FeesMode.Default,
            )
        }
        _state.update { state ->
            state.copy(
                transactionFeesInfo = state.transactionFeesInfo?.copy(
                    transactionFees = state.transactionFeesInfo.transactionFees.copy(
                        feePaddingAmount = null,
                        tipPercentage = null
                    )
                ),
                sheetState = Sheet.CustomizeFees(
                    feePayerMode = (state.sheetState as Sheet.CustomizeFees).feePayerMode,
                    feesMode = Sheet.CustomizeFees.FeesMode.Default,
                    feesInfo = requireNotNull(state.transactionFeesInfo)
                )
            )
        }
    }

    fun onViewAdvancedModeClick() {
        data.update { it.copy(latestFeesMode = Sheet.CustomizeFees.FeesMode.Advanced) }
        _state.update { state ->
            state.copy(
                sheetState = Sheet.CustomizeFees(
                    feePayerMode = (state.sheetState as Sheet.CustomizeFees).feePayerMode,
                    feesMode = Sheet.CustomizeFees.FeesMode.Advanced,
                    feesInfo = requireNotNull(state.transactionFeesInfo)
                )
            )
        }
    }

    private fun fetchXrdPrice() {
        viewModelScope.launch {
            getFiatValueUseCase.forXrd()
                .onSuccess { fiatPrice ->
                    _state.update { state ->
                        state.copy(
                            transactionFeesInfo = state.transactionFeesInfo?.copy(
                                transactionFees = state.transactionFeesInfo.transactionFees.copy(
                                    xrdFiatPrice = fiatPrice
                                )
                            )
                        )
                    }
                }
        }
    }

    private fun updateFeePayers(feePayers: TransactionFeePayers) {
        data.update { it.copy(feePayers = feePayers) }

        val transactionFeesInfo = TransactionReviewViewModel.State.TransactionFeesInfo(
            transactionFees = _state.value.transactionFeesInfo?.transactionFees ?: TransactionFees(),
            isSelectedFeePayerInvolvedInTransaction = isSelectedFeePayerInvolvedInTransaction(feePayers.selectedAccountAddress),
            noFeePayerSelected = feePayers.selectedAccountAddress == null,
            isBalanceInsufficientToPayTheFee = isBalanceInsufficientToPayTheFee()
        )

        _state.update {
            it.copy(
                transactionFeesInfo = transactionFeesInfo,
                isSubmitEnabled = it.previewType != PreviewType.None && !transactionFeesInfo.isBalanceInsufficientToPayTheFee
            )
        }
    }

    private fun isBalanceInsufficientToPayTheFee(): Boolean {
        val feePayers = data.value.feePayers ?: return true
        val candidateAddress = feePayers.selectedAccountAddress ?: return true
        val transactionFees = _state.value.transactionFeesInfo?.transactionFees ?: return true

        val xrdInCandidateAccount = feePayers.candidates.find {
            it.account.address == candidateAddress
        }?.xrdAmount.orZero()

        // Calculate how many XRD have been used from accounts withdrawn from
        // In cases were it is not a transfer type, then it means the user
        // will not spend any other XRD rather than the ones spent for the fees
        val xrdUsed = when (val previewType = _state.value.previewType) {
            is PreviewType.Transfer.GeneralTransfer -> {
                val candidateAddressWithdrawn = previewType.from.find { it.address == candidateAddress }
                if (candidateAddressWithdrawn != null) {
                    val xrdResourceWithdrawn = candidateAddressWithdrawn.resources.map {
                        it.transferable
                    }.filterIsInstance<TransferableAsset.Fungible.Token>().find { it.resource.isXrd }

                    xrdResourceWithdrawn?.amount.orZero()
                } else {
                    0.toDecimal192()
                }
            }
            // On-purpose made this check exhaustive, future types may involve accounts spending XRD
            is PreviewType.AccountsDepositSettings -> 0.toDecimal192()
            is PreviewType.NonConforming -> 0.toDecimal192()
            is PreviewType.None -> 0.toDecimal192()
            is PreviewType.UnacceptableManifest -> 0.toDecimal192()
            is PreviewType.Transfer.Pool -> 0.toDecimal192()
            is PreviewType.Transfer.Staking -> 0.toDecimal192()
        }

        return xrdInCandidateAccount - xrdUsed < transactionFees.transactionFeeToLock
    }

    private fun isSelectedFeePayerInvolvedInTransaction(selectedAccountAddress: AccountAddress?): Boolean {
        return selectedAccountAddress?.let { data.value.feePayerCandidates.contains(it) } ?: false
    }

    private fun switchToFeePayerSelection() {
        _state.update { state ->
            val transactionFeesInfo = state.transactionFeesInfo ?: return@update state
            val feePayers = data.value.feePayers

            state.copy(
                selectedFeePayerInput = TransactionReviewViewModel.State.SelectFeePayerInput(
                    preselectedCandidate = feePayers?.candidates?.firstOrNull { it.account.address == feePayers.selectedAccountAddress },
                    candidates = feePayers?.candidates.orEmpty().toPersistentList(),
                    fee = transactionFeesInfo.transactionFees.transactionFeeToLock.formatted()
                )
            )
        }
    }
}
