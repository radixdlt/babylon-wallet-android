package com.babylon.wallet.android.presentation.transaction.fees

import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.formattedTextField
import com.radixdlt.sargon.extensions.isPositive
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.negative
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.parseFromTextField
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192

data class TransactionFees(
    private val nonContingentFeeLock: Decimal192 = 0.toDecimal192(),
    private val networkExecution: Decimal192 = 0.toDecimal192(),
    private val networkFinalization: Decimal192 = 0.toDecimal192(),
    private val networkStorage: Decimal192 = 0.toDecimal192(),
    private val royalties: Decimal192 = 0.toDecimal192(),
    private val guaranteesCount: Int = 0,
    private val notaryIsSignatory: Boolean = true,
    private val includeLockFee: Boolean = false,
    private val signersCount: Int = 0,
    private val feePaddingAmount: String? = null,
    private val tipPercentage: String? = null,
    val isNetworkCongested: Boolean = false
) {

    // ********* WALLET ADDED FEES *********
    private val guaranteesCost: Decimal192
        get() = guaranteesCount.toDecimal192() * FUNGIBLE_GUARANTEE_INSTRUCTION_COST

    private val notarizingCost: Decimal192
        get() = if (notaryIsSignatory) NOTARIZING_COST_WHEN_NOTARY_IS_SIGNATORY else NOTARIZING_COST

    private val lockFeeCost: Decimal192
        get() = if (includeLockFee) LOCK_FEE_INSTRUCTION_COST else 0.toDecimal192()

    private val signaturesCost: Decimal192
        get() = signersCount.toDecimal192() * SIGNATURE_COST

    // ********* DEFAULT *********
    private val networkFee: Decimal192
        get() {
            val cost = totalExecutionCost + networkFinalization + networkStorage

            return cost + PERCENT_15 * cost
        }

    /**
     * Wallet added fee should be added to Execution cost
     */
    private val totalExecutionCost: Decimal192
        get() = networkExecution +
            guaranteesCost +
            signaturesCost +
            lockFeeCost +
            notarizingCost

    /**
     * Network Fee displayed = Network Fee - Non-contingent lock or null if negative or 0 fee applicable
     */
    val networkFeeDisplayed: String?
        get() = if ((networkFee - nonContingentFeeLock).isPositive) {
            (networkFee - nonContingentFeeLock).formatted()
        } else {
            null
        }

    /**
     * Royalty Fee displayed = Royalty Fee - abs(Non-contingent lock - Network Fee) or 0 if negative
     */
    val defaultRoyaltyFeesDisplayed: String
        get() = (royalties - (nonContingentFeeLock - networkFee).clamped).clamped.formatted()

    val noDefaultRoyaltiesDue: Boolean
        get() = defaultRoyaltyFeesDisplayed == "0"

    val defaultTransactionFee: Decimal192
        get() = (networkFee + royalties - nonContingentFeeLock).clamped

    // ********* ADVANCED *********

    val networkExecutionCost: String
        get() = totalExecutionCost.formatted()

    val networkFinalizationCost: String
        get() = networkFinalization.formatted()

    val networkStorageCost: String
        get() = networkStorage.formatted()

    val royaltiesCost: String
        get() = royalties.formatted()

    val noRoyaltiesCostDue: Boolean
        get() = royaltiesCost == "0"

    /**
     * This is negative amount (if greater than zero) representation of nonContingentLock for Paid by dApps section
     */
    val paidByDApps: String
        get() = nonContingentFeeLock.negative().formatted()

    /**
     * (tip % entered by the user) x (NETWORK EXECUTION + NETWORK FINALIZATION)
     */
    @Suppress("MagicNumber")
    val effectiveTip: Decimal192
        get() {
            val tipPercentage = tipPercentageNumber?.toLong()?.toDecimal192() ?: return 0.toDecimal192()

            return (tipPercentage / 100.toDecimal192()) * (totalExecutionCost + networkFinalization)
        }

    /**
     * Finalized fee to lock for the transaction
     **/
    val transactionFeeToLock: Decimal192
        get() = totalExecutionCost +
            networkFinalization +
            effectiveTip +
            networkStorage +
            feePaddingAmountForCalculation +
            royalties -
            nonContingentFeeLock

    /**
     * default should be the XRD amount corresponding to 15% of (EXECUTION + FINALIZATION
     */
    private val defaultPadding: Decimal192 = PERCENT_15 * (totalExecutionCost + networkFinalization + networkStorage)

    private val tipPercentageNumber: UShort?
        get() = tipPercentage?.toUShortOrNull()

    @Suppress("MagicNumber")
    val feePaddingAmountToDisplay: String
        get() = feePaddingAmount
            ?: defaultPadding.formattedTextField()

    val feePaddingAmountForCalculation: Decimal192
        get() = if (feePaddingAmount.isNullOrEmpty()) {
            defaultPadding
        } else {
            Decimal192.Companion.parseFromTextField(feePaddingAmount).decimal.orZero()
        }

    val tipPercentageToDisplay: String
        get() = tipPercentage ?: "0"

    val tipPercentageForTransaction: UShort
        get() = tipPercentageNumber ?: TransactionConfig.TIP_PERCENTAGE

    companion object {
        private val PERCENT_15 = 0.15.toDecimal192()

        private val LOCK_FEE_INSTRUCTION_COST: Decimal192 = 0.08581566997.toDecimal192()
        private val FUNGIBLE_GUARANTEE_INSTRUCTION_COST: Decimal192 = 0.00908532837.toDecimal192()
        private val SIGNATURE_COST: Decimal192 = 0.01109974758.toDecimal192()
        private val NOTARIZING_COST: Decimal192 = 0.0081393944.toDecimal192()
        private val NOTARIZING_COST_WHEN_NOTARY_IS_SIGNATORY: Decimal192 = 0.0084273944.toDecimal192()
    }
}
