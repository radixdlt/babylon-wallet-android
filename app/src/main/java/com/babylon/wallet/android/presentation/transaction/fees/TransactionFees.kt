package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.data.transaction.TransactionConfig
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

data class TransactionFees(
    private val nonContingentFeeLock: BigDecimal = BigDecimal.ZERO,
    private val networkExecution: BigDecimal = BigDecimal.ZERO,
    private val networkFinalization: BigDecimal = BigDecimal.ZERO,
    private val networkStorage: BigDecimal = BigDecimal.ZERO,
    private val royalties: BigDecimal = BigDecimal.ZERO,
    private val feePaddingAmount: String? = null,
    private val tipPercentage: BigDecimal? = null,
    val isNetworkCongested: Boolean = false
) {
    // ********* DEFAULT *********
    private val networkFee: BigDecimal
        get() = networkExecution
            .add(networkFinalization)
            .add(networkStorage)

    /**
     * Network Fee displayed = Network Fee - Non-contingent lock or null if negative or 0 fee applicable
     */
    val networkFeeDisplayed: String?
        get() = if (networkFee.subtract(nonContingentFeeLock) > BigDecimal.ZERO) {
            networkFee.subtract(nonContingentFeeLock).displayableQuantity()
        } else {
            null
        }

    /**
     * Royalty Fee displayed = Royalty Fee - abs(Non-contingent lock - Network Fee) or 0 if negative
     */
    val defaultRoyaltyFeesDisplayed: String
        get() = BigDecimal.ZERO.max(
            royalties.subtract(
                BigDecimal.ZERO.max(
                    nonContingentFeeLock.subtract(networkFee)
                )
            )
        ).displayableQuantity()

    val noDefautRoyaltiesDue: Boolean
        get() = defaultRoyaltyFeesDisplayed == "0"

    val defaultTransactionFee: BigDecimal
        get() = BigDecimal.ZERO.max(
            networkFee.add(
                PERCENT_15.multiply(networkFee)
            )
                .add(royalties)
                .subtract(nonContingentFeeLock)
        )

    // ********* ADVANCED *********

    val networkExecutionCost: String
        get() = networkExecution.displayableQuantity()

    val networkFinalizationCost: String
        get() = networkFinalization.displayableQuantity()

    val networkStorageCost: String
        get() = networkStorage.displayableQuantity()

    val royaltiesCost: String
        get() = royalties.displayableQuantity()

    val noRoyaltiesCostDue: Boolean
        get() = royaltiesCost == "0"

    /**
     * (tip % entered by the user) x (NETWORK EXECUTION + NETWORK FINALIZATION)
     */
    @Suppress("MagicNumber")
    val effectiveTip: BigDecimal
        get() = tipPercentage?.divide(
            BigDecimal(100)
        )?.multiply(
            networkExecution.add(
                networkFinalization
            )
        ) ?: BigDecimal.ZERO

    /**
     * Finalized fee to lock for the transaction
     **/
    val transactionFeeToLock: BigDecimal
        get() = networkExecution
            .add(networkFinalization)
            .add(effectiveTip)
            .add(networkStorage)
            .add(feePaddingAmountToDisplay)
            .add(royalties)

    /**
     * default should be the XRD amount corresponding to 15% of (EXECUTION + FINALIZATION
     */
    val feePaddingAmountToDisplay: BigDecimal
        get() = if (feePaddingAmount.isNullOrEmpty()) {
            PERCENT_15
                .multiply(
                    networkExecution.add(networkFinalization)
                )
        } else {
            feePaddingAmount.toBigDecimal()
        }

    val tipPercentageToDisplay: String
        get() = tipPercentage?.toPlainString() ?: "0"

    val tipPercentageForTransaction: UShort
        get() = tipPercentage?.toLong()?.toUShort()
            ?: TransactionConfig.TIP_PERCENTAGE

    val hasNetworkOrTipBeenSetup: Boolean
        get() = feePaddingAmount != null || tipPercentage != null

    companion object {
        private val PERCENT_15 = BigDecimal(0.15)
    }
}
