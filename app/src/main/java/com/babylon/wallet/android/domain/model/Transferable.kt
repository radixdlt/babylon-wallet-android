package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network

sealed interface Transferable {
    val resource: TransferableResource
}

sealed interface TransferableResource {

    val resourceAddress: String

    data class Amount(val resource: Resource.FungibleResource): TransferableResource {
        override val resourceAddress: String
            get() = resource.resourceAddress
    }

    data class NFTs(val items: List<Resource.NonFungibleResource.Item>): TransferableResource {
        override val resourceAddress: String
            get() = items.firstOrNull()?.collectionAddress.orEmpty()
    }
}

data class DepositingTransferableResource(
    val guaranteeType: GuaranteeType = GuaranteeType.Guaranteed,
    override val resource: TransferableResource
): Transferable

data class WithdrawingTransferableResource(
    override val resource: TransferableResource
): Transferable


sealed interface GuaranteeType {
    object Guaranteed: GuaranteeType
    data class Predicted(
        val instructionIndex: Long
    ): GuaranteeType
}

sealed interface AccountWithTransferableResources {
    data class Owned(
        val account: Network.Account,
        val resources: List<Transferable>
    ): AccountWithTransferableResources

    data class Other(
        val address: String,
        val resources: List<Transferable>
    ): AccountWithTransferableResources
}
