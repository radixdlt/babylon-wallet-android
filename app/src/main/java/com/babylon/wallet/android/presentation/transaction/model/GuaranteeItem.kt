package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.formattedTextField
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.parseFromTextField
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192

@Suppress("MagicNumber")
data class GuaranteeItem(
    val account: InvolvedAccount,
    val transferable: Transferable.FungibleType,
    val typedPercent: String // User's typed amount
) {

    val accountAddress: AccountAddress
        get() = account.address

    // The decimal representation of the user's typed amount. Can be null if the input is malformed.
    private val typedPercentDecimal: Decimal192? = Decimal192.parseFromTextField(textFieldString = typedPercent).decimal

    val isInputValid: Boolean = typedPercentDecimal != null

    val isDecreaseAllowed: Boolean = typedPercentDecimal != null && typedPercentDecimal > zero

    // The updated amount with the guarantee.
    val updatedAmount: FungibleAmount.Predicted = (transferable.amount as FungibleAmount.Predicted).copy(
        offset = typedPercentDecimal.orZero() / hundred
    )

    fun increase(): GuaranteeItem {
        val newPercent = (typedPercentDecimal.orZero() + changeOffset).rounded(decimalPlaces = 1u)

        return copy(typedPercent = newPercent.formattedTextField())
    }

    fun decrease(): GuaranteeItem {
        val newPercent = (typedPercentDecimal.orZero() - changeOffset).clamped.rounded(decimalPlaces = 1u)

        return copy(typedPercent = newPercent.formattedTextField())
    }

    fun change(newTypedPercent: String): GuaranteeItem =
        copy(typedPercent = Decimal192.parseFromTextField(newTypedPercent).input)

    fun isTheSameGuaranteeItem(with: GuaranteeItem): Boolean = account.address == with.account.address &&
            transferable.resourceAddress == with.transferable.resourceAddress

    companion object {
        fun from(
            involvedAccount: InvolvedAccount,
            transferable: Transferable.FungibleType
        ): GuaranteeItem? {
            val predictedAmount = (transferable.amount as? FungibleAmount.Predicted) ?: return null

            return GuaranteeItem(
                account = involvedAccount,
                transferable = transferable,
                typedPercent = (predictedAmount.offset * hundred).formattedTextField()
            )
        }

        private val changeOffset = 1.toDecimal192()
        private val hundred = 100.toDecimal192()
        private val zero = 0.toDecimal192()
    }
}
