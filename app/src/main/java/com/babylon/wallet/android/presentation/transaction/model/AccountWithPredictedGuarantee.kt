package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.domain.model.TransferableAsset
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.formattedTextField
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.parseFromTextField
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.roundedWith

@Suppress("MagicNumber")
sealed interface AccountWithPredictedGuarantee {
    val address: AccountAddress
    val transferable: TransferableAsset.Fungible
    val instructionIndex: Long
    val guaranteeAmountString: String

    val guaranteeOffsetDecimal: Decimal192
        get() = Decimal192.Companion.parseFromTextField(guaranteeAmountString).decimal.orZero() / 100.toDecimal192()

    val guaranteedAmount: Decimal192
        get() = (transferable.amount * guaranteeOffsetDecimal).roundedWith(divisibility)

    private val divisibility: Divisibility?
        get() = when (val asset = transferable) {
            is TransferableAsset.Fungible.Token -> {
                asset.resource.divisibility
            }

            is TransferableAsset.Fungible.LSUAsset -> {
                asset.resource.divisibility
            }

            is TransferableAsset.Fungible.PoolUnitAsset -> {
                asset.resource.divisibility
            }
        }

    fun increase(): AccountWithPredictedGuarantee {
        val newOffset = ((guaranteeOffsetDecimal + changeOffset) * 100.toDecimal192()).rounded(decimalPlaces = 1u)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.formattedTextField())
            is Owned -> copy(guaranteeAmountString = newOffset.formattedTextField())
        }
    }

    fun decrease(): AccountWithPredictedGuarantee {
        val newOffset = ((guaranteeOffsetDecimal - changeOffset).clamped * 100.toDecimal192()).rounded(decimalPlaces = 1u)
        return when (this) {
            is Other -> copy(guaranteeAmountString = newOffset.formattedTextField())
            is Owned -> copy(guaranteeAmountString = newOffset.formattedTextField())
        }
    }

    fun change(amount: String): AccountWithPredictedGuarantee {
        val value = amount.toFloatOrNull() ?: 0f
        return if (value >= 0f) {
            when (this) {
                is Other -> copy(guaranteeAmountString = amount)
                is Owned -> copy(guaranteeAmountString = amount)
            }
        } else {
            this
        }
    }

    fun isTheSameGuaranteeItem(with: AccountWithPredictedGuarantee): Boolean = address == with.address &&
        transferable.resourceAddress == with.transferable.resourceAddress

    data class Owned(
        val account: Account,
        override val transferable: TransferableAsset.Fungible,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(
        override val address: AccountAddress,
        override val transferable: TransferableAsset.Fungible,
        override val instructionIndex: Long,
        override val guaranteeAmountString: String
    ) : AccountWithPredictedGuarantee

    companion object {
        private val changeOffset = 0.01.toDecimal192()
    }
}
