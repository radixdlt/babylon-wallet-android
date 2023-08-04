package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.data.transaction.TransactionConfig
import rdx.works.core.displayableQuantity
import java.lang.NumberFormatException
import java.math.BigDecimal

data class TransactionFees(
    private val networkFee: BigDecimal = BigDecimal.ZERO,
    private val royaltyFee: BigDecimal = BigDecimal.ZERO,
    private val nonContingentFeeLock: BigDecimal = BigDecimal.ZERO,
    private val defaultTip: BigDecimal = BigDecimal.ZERO,
    private val networkAndRoyaltyFees: String? = null,
    private val tipPercentage: String? = null,
    val isNetworkCongested: Boolean = false
) {
    // ********* DEFAULT *********

    val defaultNetworkFee: String
        get() = networkFee.displayableQuantity()

    val defaultRoyaltyFee: String
        get() = royaltyFee.displayableQuantity()

    val defaultTipToDisplay: String
        get() = defaultTip.displayableQuantity()

    val defaultTransactionFee: BigDecimal
        get() = networkFee
            .multiply(MARGIN)
            .add(royaltyFee)
            .subtract(nonContingentFeeLock)


    // ********* ADVANCED *********
    /**
     * Finalized fee to lock for the transaction
     * the TRANSACTION FEE at the bottom should be calculated as:
     *  "XRD to Lock for Network & Royalty Fees" + ( "Validator Tip % to Lock" x ( network fee x 1.15 ) ).
     * And then that number is exactly what should be locked from the user's selected account.
     */
    val transactionFeeToLock: BigDecimal
        get() = if (networkAndRoyaltyFees != null && tipPercentage != null) {
            // Both tip and network&royalty fee has changed
            val tipPercentageBigDecimal = try {
                tipPercentage.toBigDecimal().divide(BigDecimal(100))
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
            networkAndRoyaltyFees.toBigDecimal()
                .add(
                    tipPercentageBigDecimal
                        .multiply(networkFee)
                        .multiply(MARGIN)
                )
        } else if (networkAndRoyaltyFees == null && tipPercentage != null) {
            // Just percentage has changed
            val tipPercentageBigDecimal = try {
                tipPercentage.toBigDecimal().divide(BigDecimal(100))
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
            defaultTransactionFee
                .add(
                    tipPercentageBigDecimal
                        .multiply(networkFee)
                        .multiply(MARGIN)
                )
        } else if (networkAndRoyaltyFees != null && tipPercentage == null) {
            // Just network&royalty fee has changed, tip remains 0%
            try {
                networkAndRoyaltyFees.toBigDecimal()
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        } else {
            defaultTransactionFee
        }

    val networkAndRoyaltyFeesToDisplay: String
        get() = networkAndRoyaltyFees ?: networkFee
            .multiply(MARGIN)
            .add(royaltyFee).displayableQuantity()

    val tipPercentageToDisplay: String
        get() = tipPercentage ?: "0"

    val tipPercentageForTransaction: UShort
        get() = tipPercentage?.toBigDecimal()
            ?.toLong()?.toUShort()
            ?: TransactionConfig.TIP_PERCENTAGE

    companion object {
        private val MARGIN = BigDecimal(1.15)
    }
}
