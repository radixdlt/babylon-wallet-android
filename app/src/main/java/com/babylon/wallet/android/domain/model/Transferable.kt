package com.babylon.wallet.android.domain.model

import androidx.annotation.FloatRange
import java.math.BigDecimal

sealed interface Transferable {
    val transferable: TransferableResource

    /**
     * Will exist only for
     * 1. depositing trasnferrables
     * 2. with guarantee type Predicted
     * 3. and TransferableResource.Amount
     */
    val guaranteedAmount: BigDecimal?
        get() {
            return when (this) {
                is Depositing -> {
                    val predicted = guaranteeType as? GuaranteeType.Predicted ?: return null

                    when (val transferable = transferable) {
                        is TransferableResource.Amount -> transferable.amount * predicted.guaranteePercent.toBigDecimal()
                        is TransferableResource.NFTs -> null
                    }
                }
                is Withdrawing -> null
            }
        }

    data class Depositing(
        override val transferable: TransferableResource,
        val guaranteeType: GuaranteeType = GuaranteeType.Guaranteed
    ): Transferable

    data class Withdrawing(
        override val transferable: TransferableResource
    ): Transferable

    fun updateGuarantee(
        @FloatRange(from = 0.0, to = 1.0)
        guaranteePercent: Float
    ): Transferable {
        return when (this) {
            is Depositing -> {
                val predicted = (guaranteeType as? GuaranteeType.Predicted) ?: return this

                copy(guaranteeType = predicted.copy(guaranteePercent = guaranteePercent))
            }
            is Withdrawing -> this
        }
    }
}

sealed interface GuaranteeType {
    object Guaranteed: GuaranteeType
    data class Predicted(
        val instructionIndex: Long,
        @FloatRange(from = 0.0, to = 1.0)
        val guaranteePercent: Float = 1f
    ): GuaranteeType
}

sealed interface TransferableResource {

    val resource: Resource
    val resourceAddress: String
        get() = resource.resourceAddress

    data class Amount(
        val amount: BigDecimal,
        override val resource: Resource.FungibleResource
    ): TransferableResource

    data class NFTs(override val resource: Resource.NonFungibleResource): TransferableResource
}
