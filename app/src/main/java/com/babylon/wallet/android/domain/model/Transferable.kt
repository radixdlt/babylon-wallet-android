package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

sealed interface Transferable {
    val transferable: TransferableResource

    data class Depositing(
        override val transferable: TransferableResource,
        val guaranteeType: GuaranteeType = GuaranteeType.Guaranteed
    ): Transferable

    data class Withdrawing(
        override val transferable: TransferableResource
    ): Transferable
}

sealed interface GuaranteeType {
    object Guaranteed: GuaranteeType
    data class Predicted(
        val instructionIndex: Long
    ): GuaranteeType
}

sealed interface TransferableResource {

    val resourceAddress: String

    data class Amount(
        val amount: BigDecimal,
        val resource: Resource.FungibleResource
    ): TransferableResource {
        override val resourceAddress: String
            get() = resource.resourceAddress
    }

    data class NFTs(val items: List<Resource.NonFungibleResource.Item>): TransferableResource {
        override val resourceAddress: String
            get() = items.firstOrNull()?.collectionAddress.orEmpty()
    }
}
