package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.signing.NotaryAndSigners
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.analysis.Analysis
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.toUnit
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface TransactionFeesDelegate {

    fun onCustomizeClick()

    fun onChangeFeePayerClick()

    fun onSelectFeePayerClick()

    fun onFeePayerChanged(selectedFeePayer: TransactionFeePayers.FeePayerCandidate)

    fun onFeePayerSelected()

    fun onFeePayerSelectionDismissRequest()

    fun onFeePaddingAmountChanged(feePaddingAmount: String)

    fun onTipPercentageChanged(tipPercentage: String)

    fun onViewDefaultModeClick()

    fun onViewAdvancedModeClick()
}

@Suppress("TooManyFunctions")
class TransactionFeesDelegateImpl @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>(),
    TransactionFeesDelegate {

    private val logger = Timber.tag("TransactionFees")

    suspend fun resolveFees(analysis: Analysis): Result<Unit> {
        val executionSummary = (analysis.summary as? Summary.FromExecution)?.summary ?: error(
            "Fees resolver should be called only on normal transactions which are resolved with an ExecutionSummary"
        )
        _state.update { it.copy(fees = TransactionReviewViewModel.State.Fees(isNetworkFeeLoading = true)) }

        val transactionFees = TransactionFees.from(
            summary = executionSummary,
            notaryAndSigners = NotaryAndSigners(
                signers = analysis.signers,
                ephemeralNotaryPrivateKey = data.value.ephemeralNotaryPrivateKey
            ),
            previewType = _state.value.previewType
        )
        observeFeesChanges()

        return searchFeePayersUseCase(
            feePayerCandidates = executionSummary.feePayerCandidates(),
            lockFee = transactionFees.defaultTransactionFee
        ).onSuccess { feePayers ->
            _state.update { state ->
                state.copy(
                    fees = state.fees?.copy(isNetworkFeeLoading = false)
                )
            }
            data.update { data ->
                data.copy(
                    feePayers = feePayers,
                    transactionFees = transactionFees
                )
            }
            fetchXrdPrice()
        }.onFailure { throwable ->
            logger.w(throwable)

            _state.update {
                it.copy(
                    isLoading = false,
                    previewType = PreviewType.None,
                    error = TransactionErrorMessage(throwable)
                )
            }
        }.toUnit()
    }

    override fun onCustomizeClick() {
        val feesState = _state.value.fees ?: return

        if (feesState.transactionFees.defaultTransactionFee.isZero) {
            // None required
            _state.update { state ->
                state.copy(
                    sheetState = Sheet.CustomizeFees(
                        feePayerMode = Sheet.CustomizeFees.FeePayerMode.NoFeePayerRequired,
                        feesMode = data.value.latestFeesMode,
                        transactionFees = feesState.transactionFees,
                        properties = feesState.properties
                    )
                )
            }
        } else {
            val feePayers = data.value.feePayers ?: return
            if (feePayers.selectedAccountAddress != null) {
                viewModelScope.launch {
                    // Candidate selected
                    val feePayerCandidate = getProfileUseCase().activeAccountOnCurrentNetwork(
                        withAddress = feePayers.selectedAccountAddress
                    ) ?: return@launch
                    _state.update { state ->
                        state.copy(
                            sheetState = Sheet.CustomizeFees(
                                feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                                    feePayerCandidate = feePayerCandidate
                                ),
                                feesMode = data.value.latestFeesMode,
                                transactionFees = requireNotNull(state.fees?.transactionFees),
                                properties = requireNotNull(state.fees?.properties)
                            )
                        )
                    }
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
                            transactionFees = requireNotNull(state.fees?.transactionFees),
                            properties = requireNotNull(state.fees?.properties)
                        )
                    )
                }
            }
        }
    }

    override fun onChangeFeePayerClick() {
        switchToFeePayerSelection()
    }

    override fun onSelectFeePayerClick() {
        switchToFeePayerSelection()
    }

    override fun onFeePayerChanged(selectedFeePayer: TransactionFeePayers.FeePayerCandidate) {
        _state.update { state ->
            state.copy(
                fees = state.fees?.copy(
                    selectedFeePayerInput = state.fees.selectedFeePayerInput?.copy(
                        preselectedCandidate = selectedFeePayer
                    )
                )
            )
        }
    }

    override fun onFeePayerSelected() {
        val feesState = _state.value.fees ?: return
        val selectedFeePayerAccount = feesState.selectedFeePayerInput?.preselectedCandidate?.account ?: return
        val feePayers = data.value.feePayers ?: return

        val signersCount = data.value.signers.count()

        val updatedFeePayers = feePayers.copy(
            selectedAccountAddress = selectedFeePayerAccount.address,
            candidates = feePayers.candidates
        )
        data.update { it.copy(feePayers = updatedFeePayers) }

        val customizeFeesSheet = _state.value.sheetState as? Sheet.CustomizeFees ?: return
        val updatedSignersCount = if (isSelectedFeePayerInvolvedInTransaction(selectedFeePayerAccount.address)) {
            signersCount
        } else {
            signersCount + 1
        }

        data.update {
            it.copy(
                transactionFees = feesState.transactionFees.copy(
                    signersCount = updatedSignersCount
                )
            )
        }
        _state.update {
            it.copy(
                sheetState = customizeFeesSheet.copy(
                    feePayerMode = Sheet.CustomizeFees.FeePayerMode.FeePayerSelected(
                        feePayerCandidate = selectedFeePayerAccount
                    )
                )
            )
        }
    }

    override fun onFeePayerSelectionDismissRequest() {
        _state.update { state ->
            state.copy(
                fees = state.fees?.copy(
                    selectedFeePayerInput = null
                )
            )
        }
    }

    override fun onFeePaddingAmountChanged(feePaddingAmount: String) {
        val transactionFees = data.value.transactionFees ?: return
        val feePayers = data.value.feePayers ?: return
        val newTransactionFees = transactionFees.copy(
            feePaddingAmount = feePaddingAmount
        )

        data.update { data ->
            data.copy(
                transactionFees = newTransactionFees,
                feePayers = feePayers.copy(
                    candidates = feePayers.candidates.map {
                        it.copy(
                            hasEnoughBalance = it.xrdAmount >= newTransactionFees.transactionFeeToLock
                        )
                    }
                )
            )
        }
    }

    @Suppress("MagicNumber")
    override fun onTipPercentageChanged(tipPercentage: String) {
        data.update { data ->
            data.copy(
                transactionFees = data.transactionFees?.copy(
                    tipPercentage = tipPercentage.filter { it.isDigit() }
                )
            )
        }
    }

    override fun onViewDefaultModeClick() {
        _state.update { state ->
            state.copy(
                sheetState = (state.sheetState as Sheet.CustomizeFees).copy(
                    feesMode = Sheet.CustomizeFees.FeesMode.Default
                )
            )
        }
        data.update { data ->
            data.copy(
                latestFeesMode = Sheet.CustomizeFees.FeesMode.Default,
                transactionFees = data.transactionFees?.copy(
                    feePaddingAmount = null,
                    tipPercentage = null
                )
            )
        }
    }

    override fun onViewAdvancedModeClick() {
        data.update { data -> data.copy(latestFeesMode = Sheet.CustomizeFees.FeesMode.Advanced) }
        _state.update { state ->
            state.copy(
                sheetState = (state.sheetState as Sheet.CustomizeFees).copy(
                    feesMode = Sheet.CustomizeFees.FeesMode.Advanced
                )
            )
        }
    }

    private fun fetchXrdPrice() {
        viewModelScope.launch {
            getFiatValueUseCase.forXrd()
                .onSuccess { fiatPrice ->
                    data.update { data ->
                        data.copy(
                            transactionFees = data.transactionFees?.copy(
                                xrdFiatPrice = fiatPrice
                            )
                        )
                    }
                }
        }
    }

    private fun observeFeesChanges() {
        viewModelScope.launch {
            data.mapNotNull {
                val feePayers = it.feePayers ?: return@mapNotNull null
                val transactionFees = it.transactionFees ?: return@mapNotNull null
                feePayers to transactionFees
            }
                .distinctUntilChanged()
                .collect { feePayersAndTransactionFees ->
                    _state.update { state ->
                        val feePayers = feePayersAndTransactionFees.first
                        val transactionFees = feePayersAndTransactionFees.second

                        val properties = TransactionReviewViewModel.State.Fees.Properties(
                            isSelectedFeePayerInvolvedInTransaction = isSelectedFeePayerInvolvedInTransaction(
                                feePayers.selectedAccountAddress
                            ),
                            noFeePayerSelected = feePayers.selectedAccountAddress == null,
                            isBalanceInsufficientToPayTheFee = isBalanceInsufficientToPayTheFee(
                                feePayers,
                                transactionFees.transactionFeeToLock
                            )
                        )

                        state.copy(
                            fees = state.fees?.copy(
                                properties = properties,
                                transactionFees = transactionFees,
                            ),
                            sheetState = (state.sheetState as? Sheet.CustomizeFees)?.copy(
                                transactionFees = transactionFees,
                                properties = properties
                            ) ?: state.sheetState,
                            isSubmitEnabled = state.previewType != PreviewType.None && !properties.isBalanceInsufficientToPayTheFee
                        )
                    }
                }
        }
    }

    private fun isBalanceInsufficientToPayTheFee(feePayers: TransactionFeePayers, feeToLock: Decimal192): Boolean {
        val candidateAddress = feePayers.selectedAccountAddress ?: return true

        if (feeToLock.isZero) {
            return false
        }

        val xrdInCandidateAccount = feePayers.candidates.find {
            it.account.address == candidateAddress
        }?.xrdAmount.orZero()

        // Calculate how many XRD have been used from accounts withdrawn from
        // In cases were it is not a transfer type, then it means the user
        // will not spend any other XRD rather than the ones spent for the fees
        val xrdUsed = when (val previewType = _state.value.previewType) {
            is PreviewType.Transaction -> {
                val candidateAddressWithdrawn = previewType.from.find { it.account.address == candidateAddress }
                if (candidateAddressWithdrawn != null) {
                    val xrdAmount = candidateAddressWithdrawn
                        .transferables
                        .filterIsInstance<Transferable.FungibleType.Token>()
                        .find { it.asset.resource.isXrd }?.amount

                    when (xrdAmount) {
                        null -> 0.toDecimal192()
                        is CountedAmount.Exact -> xrdAmount.amount
                        is CountedAmount.Predicted -> xrdAmount.estimated
                        else -> 0.toDecimal192() // Should not go through these cases. Fees are calculated only on regular transactions.
                    }
                } else {
                    0.toDecimal192()
                }
            }
            // On-purpose made this check exhaustive, future types may involve accounts spending XRD
            is PreviewType.AccountsDepositSettings,
            is PreviewType.NonConforming,
            is PreviewType.None,
            is PreviewType.PreAuthTransaction,
            is PreviewType.UnacceptableManifest -> 0.toDecimal192()
        }

        return xrdInCandidateAccount - xrdUsed < feeToLock
    }

    private fun isSelectedFeePayerInvolvedInTransaction(selectedAccountAddress: AccountAddress?): Boolean {
        val executionSummary = (data.value.summary as? Summary.FromExecution)?.summary ?: error(
            "Fees resolver should be called only on normal transactions which are resolved with an ExecutionSummary"
        )

        val candidates = executionSummary.feePayerCandidates()
        return selectedAccountAddress?.let { candidates.contains(it) } ?: true
    }

    private fun switchToFeePayerSelection() {
        _state.update { state ->
            val feePayers = data.value.feePayers

            state.copy(
                fees = state.fees?.copy(
                    selectedFeePayerInput = TransactionReviewViewModel.State.SelectFeePayerInput(
                        preselectedCandidate = feePayers?.candidates?.firstOrNull {
                            it.account.address == feePayers.selectedAccountAddress
                        },
                        candidates = feePayers?.candidates.orEmpty().toPersistentList(),
                        fee = state.fees.transactionFees.transactionFeeToLock.formatted()
                    )
                )
            )
        }
    }

    private fun ExecutionSummary.feePayerCandidates(): Set<AccountAddress> = withdrawals.keys +
        deposits.keys +
        addressesOfAccountsRequiringAuth.toSet()
}
