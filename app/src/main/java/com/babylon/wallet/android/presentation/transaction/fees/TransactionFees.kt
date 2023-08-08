package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.data.transaction.TransactionConfig
import rdx.works.core.displayableQuantity
import java.lang.NumberFormatException
import java.math.BigDecimal

data class TransactionFees(
    private val networkFee: BigDecimal = BigDecimal.ZERO,
    private val royaltyFee: BigDecimal = BigDecimal.ZERO,
    private val nonContingentFeeLock: BigDecimal = BigDecimal.ZERO,
    private val networkAndRoyaltyFees: String? = null,
    private val tipPercentage: String? = null,
    val isNetworkCongested: Boolean = false
) {
    // ********* DEFAULT *********

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
    val royaltyFeesDisplayed: String
        get() = BigDecimal.ZERO.max(
            royaltyFee.subtract(
                BigDecimal.ZERO.max(
                    nonContingentFeeLock.subtract(networkFee)
                )
            )
        ).displayableQuantity()

    val defaultTransactionFee: BigDecimal
        get() = BigDecimal.ZERO.max(
            networkFee
                .multiply(MARGIN)
                .add(royaltyFee)
                .subtract(nonContingentFeeLock)
        )

    // ********* ADVANCED *********
    /**
     * Finalized fee to lock for the transaction
     * the TRANSACTION FEE at the bottom should be calculated as:
     *  "XRD to Lock for Network & Royalty Fees" + ( "Validator Tip % to Lock" x ( network fee x 1.15 ) ).
     * And then that number is exactly what should be locked from the user's selected account.
     *    let networkFee = max(advanced.networkAndRoyaltyFee - royaltyFee, .zero)
     *   let tipAmount = networkFee * (advanced.tipPercentage / 100)
     *   let total = advanced.networkAndRoyaltyFee + tipAmount
     */
    @Suppress("MagicNumber")
    val transactionFeeToLock: BigDecimal
        get() = if (networkAndRoyaltyFees != null && tipPercentage != null) {
            // Both tip and network&royalty fee has changed
            val networkAndRoyaltyBigDecimal = networkAndRoyaltyFees.toBigDecimal()
            val networkFeeForTip = BigDecimal.ZERO.max(networkAndRoyaltyBigDecimal.subtract(royaltyFee))
            val tipAmount = networkFeeForTip.multiply(tipPercentage.toBigDecimal().divide(BigDecimal(100)))
            networkAndRoyaltyBigDecimal.add(tipAmount)
        } else if (networkAndRoyaltyFees == null && tipPercentage != null) {
            // Just percentage has changed
            val networkFeeForTip = BigDecimal.ZERO.max(defaultTransactionFee.subtract(royaltyFee))
            val tipAmount = networkFeeForTip.multiply(tipPercentage.toBigDecimal().divide(BigDecimal(100)))
            defaultTransactionFee.add(tipAmount)
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

    val hasNetworkOrTipBeenSetup: Boolean
        get() = networkAndRoyaltyFees != null || tipPercentage != null

    companion object {
        private val MARGIN = BigDecimal(1.15)
    }
}
