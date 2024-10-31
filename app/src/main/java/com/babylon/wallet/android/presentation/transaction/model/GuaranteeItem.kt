package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.radixdlt.sargon.Account
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

sealed interface InvolvedAccount {

    val address: AccountAddress

    data class Owned(val account: Account): InvolvedAccount {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(override val address: AccountAddress): InvolvedAccount
}

@Suppress("MagicNumber")
data class GuaranteeItem(
    val account: InvolvedAccount,
    val transferable: Transferable.FungibleType,
    val typedPercent: String
) {

    val updatedAmount: FungibleAmount.Predicted = (transferable.amount as FungibleAmount.Predicted).copy(
        percent = typedOffsetDecimal
    )

    val accountAddress: AccountAddress
        get() = account.address

    private val typedOffsetDecimal: Decimal192
        get() = Decimal192.parseFromTextField(typedPercent).decimal.orZero() / hundred

    fun increase(): GuaranteeItem {
        val newPercent = ((typedOffsetDecimal + changeOffset) * hundred).rounded(decimalPlaces = 1u)

        return copy(typedPercent = newPercent.formattedTextField())
    }

    fun decrease(): GuaranteeItem {
        val newPercent = ((typedOffsetDecimal - changeOffset).clamped * hundred).rounded(decimalPlaces = 1u)

        return copy(typedPercent = newPercent.formattedTextField())
    }

    fun change(newTypedPercent: String): GuaranteeItem {
        val decimal = Decimal192.parseFromTextField(newTypedPercent).decimal.orZero()

        return if (decimal >= zero) {
            copy(typedPercent = newTypedPercent)
        } else {
            this
        }
    }

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
                typedPercent = predictedAmount.percent.formattedTextField()
            )
        }

        private val changeOffset = 0.01.toDecimal192()
        private val zero = 0.toDecimal192()
        private val hundred = 100.toDecimal192()
    }
}
