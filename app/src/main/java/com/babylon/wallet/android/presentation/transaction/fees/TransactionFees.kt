package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.data.transaction.TransactionConfig
import rdx.works.core.displayableQuantity
import java.math.BigDecimal
import java.math.RoundingMode

data class TransactionFees(
    private val nonContingentFeeLock: BigDecimal = BigDecimal.ZERO,
    private val networkExecution: BigDecimal = BigDecimal.ZERO,
    private val walletAddedFee: BigDecimal = BigDecimal.ZERO,// TODO This will be updated later on
    private val networkFinalization: BigDecimal = BigDecimal.ZERO,
    private val networkStorage: BigDecimal = BigDecimal.ZERO,
    private val royalties: BigDecimal = BigDecimal.ZERO,
    private val feePaddingAmount: String? = null,
    private val tipPercentage: BigDecimal? = null,
    val isNetworkCongested: Boolean = false
) {
    // ********* DEFAULT *********
    private val networkFee: BigDecimal
        get() = networkExecutionFee
            .add(networkFinalization)
            .add(networkStorage)

    /**
     * Wallet added fee should be added to Execution cost
     */
    private val networkExecutionFee: BigDecimal
        get() = networkExecution.add(walletAddedFee)
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

    val noDefaultRoyaltiesDue: Boolean
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
        get() = networkExecutionFee.displayableQuantity()

    val networkFinalizationCost: String
        get() = networkFinalization.displayableQuantity()

    val networkStorageCost: String
        get() = networkStorage.displayableQuantity()

    val royaltiesCost: String
        get() = royalties.displayableQuantity()

    val noRoyaltiesCostDue: Boolean
        get() = royaltiesCost == "0"

    /**
     * This is negative amount (if greater than zero) representation of nonContingentLock for Paid by dApps section
     */
    val paidByDApps: String
        get() = nonContingentFeeLock.negate()?.displayableQuantity().orEmpty()

    /**
     * (tip % entered by the user) x (NETWORK EXECUTION + NETWORK FINALIZATION)
     */
    @Suppress("MagicNumber")
    val effectiveTip: BigDecimal
        get() = tipPercentage?.divide(
            BigDecimal(100)
        )?.multiply(
            networkExecutionFee.add(
                networkFinalization.add(
                    networkStorage
                )
            )
        ) ?: BigDecimal.ZERO

    /**
     * Finalized fee to lock for the transaction
     **/
    val transactionFeeToLock: BigDecimal
        get() = networkExecutionFee
            .add(networkFinalization)
            .add(effectiveTip)
            .add(networkStorage)
            .add(feePaddingAmountForCalculation)
            .add(royalties)
            .subtract(nonContingentFeeLock)

    /**
     * default should be the XRD amount corresponding to 15% of (EXECUTION + FINALIZATION
     */
    private val defaultPadding: BigDecimal = PERCENT_15
        .multiply(
            networkExecutionFee.add(
                networkFinalization.add(networkStorage)
            )
        )

    @Suppress("MagicNumber")
    val feePaddingAmountToDisplay: String
        get() = feePaddingAmount
            ?: defaultPadding
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()

    val feePaddingAmountForCalculation: BigDecimal
        get() = if (feePaddingAmount.isNullOrEmpty()) {
            defaultPadding
        } else {
            feePaddingAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }

    val tipPercentageToDisplay: String
        get() = tipPercentage?.toPlainString() ?: "0"

    val tipPercentageForTransaction: UShort
        get() = tipPercentage?.toLong()?.toUShort()
            ?: TransactionConfig.TIP_PERCENTAGE

    companion object {
        private val PERCENT_15 = BigDecimal(0.15)
    }
}
