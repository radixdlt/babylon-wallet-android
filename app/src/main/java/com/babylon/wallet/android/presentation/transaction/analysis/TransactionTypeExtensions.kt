package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.TransferableResource
import com.radixdlt.ret.ResourceSpecifier
import rdx.works.core.ret.asStr
import java.math.BigDecimal

typealias RETResources = com.radixdlt.ret.Resources
typealias RETResourcesAmount = com.radixdlt.ret.Resources.Amount
typealias RETResourcesIds = com.radixdlt.ret.Resources.Ids

//fun TransactionType.GeneralTransaction.resolveTo(
//    from: MutableMap<String, List<Withdrawing>>,
//    to: MutableMap<String, List<Transferable.Depositing>>
//) {
//    from.putAll(
//        accountWithdraws.mapValues { entry ->
//            entry.value.map { Withdrawing(transferable = it.toTransferableResource()) }
//        }
//    )
//
//    to.putAll(
//        accountDeposits.mapValues { entry ->
//            entry.value.map { source ->
//                when (source) {
//                    is Source.Guaranteed -> Transferable.Depositing(
//                        guaranteeType = GuaranteeType.Guaranteed,
//                        transferable = source.value.toTransferableResource()
//                    )
//
//                    is Source.Predicted -> Transferable.Depositing(
//                        guaranteeType = GuaranteeType.Predicted(instructionIndex = source.instructionIndex.toLong()),
//                        transferable = source.value.toTransferableResource()
//                    )
//                }
//            }
//        }
//    )
//}

//fun TransactionType.SimpleTransfer.resolveTo(
//    from: MutableMap<String, List<Withdrawing>>,
//    to: MutableMap<String, List<Transferable.Depositing>>
//) {
//    from[this.from.addressString()] = listOf(
//        Withdrawing(
//            transferable = transferred.toTransferableResource()
//        )
//    )
//    to[this.to.addressString()] = listOf(
//        Transferable.Depositing(transferable = transferred.toTransferableResource())
//    )
//}

//fun TransactionType.Transfer.resolveTo(
//    from: MutableMap<String, List<Withdrawing>>,
//    to: MutableMap<String, List<Transferable.Depositing>>
//) {
//    from[this.from.addressString()] = emptyList()
//    to.putAll(
//        transfers.mapValues { toTransfers ->
//            toTransfers.value.entries.map { resourceEntry ->
//                Transferable.Depositing(transferable = resourceEntry.value.toTransferableResource(resourceEntry.key))
//            }
//        }
//    )
//}


fun ResourceSpecifier.toTransferableResource(allResources: List<Resources>): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is ResourceSpecifier.Amount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource =  allFungibles.find { it.resourceAddress == resourceAddress.addressString() } ?: Resource.FungibleResource(
                resourceAddress = resourceAddress.addressString(),
                amount = BigDecimal.ZERO
            )
        )
        is ResourceSpecifier.Ids -> {
            val collectionItems = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }?.items.orEmpty()
            TransferableResource.NFTs(
                items = ids.map { id ->
                    collectionItems.find { it.localId == id } ?: Resource.NonFungibleResource.Item(
                        collectionAddress = this.resourceAddress.addressString(),
                        localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
                    )
                }
            )
        }
    }
}

fun RETResources.toTransferableResource(resourceAddress: String, allResources: List<Resources>): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is RETResourcesAmount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = allFungibles.find { it.resourceAddress == resourceAddress } ?: Resource.FungibleResource(
                resourceAddress = resourceAddress,
                amount = BigDecimal.ZERO
            )
        )
        is RETResourcesIds -> {
            val collectionItems = allNFTCollections.find { it.resourceAddress == resourceAddress }?.items.orEmpty()
            TransferableResource.NFTs(
                items = ids.map { id ->
                    collectionItems.find { it.localId == id } ?: Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress,
                        localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
                    )
                }
            )
        }
    }
}
