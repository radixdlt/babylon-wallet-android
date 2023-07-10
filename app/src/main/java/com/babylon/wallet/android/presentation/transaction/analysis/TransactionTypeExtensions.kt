package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.DepositingTransferableResource
import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.WithdrawingTransferableResource
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.Resources
import com.radixdlt.ret.Source
import com.radixdlt.ret.TransactionType
import rdx.works.core.ret.asStr

fun TransactionType.GeneralTransaction.resolveTo(
    from: MutableMap<String, List<WithdrawingTransferableResource>>,
    to: MutableMap<String, List<DepositingTransferableResource>>
) {
    from.putAll(
        accountWithdraws.mapValues { entry ->
            entry.value.map { WithdrawingTransferableResource(resource = it.toTransferableResource()) }
        }
    )

    to.putAll(
        accountDeposits.mapValues { entry ->
            entry.value.map { source ->
                when (source) {
                    is Source.Guaranteed -> DepositingTransferableResource(
                        guaranteeType = GuaranteeType.Guaranteed,
                        resource = source.value.toTransferableResource()
                    )

                    is Source.Predicted -> DepositingTransferableResource(
                        guaranteeType = GuaranteeType.Predicted(instructionIndex = source.instructionIndex.toLong()),
                        resource = source.value.toTransferableResource()
                    )
                }
            }
        }
    )
}

fun TransactionType.SimpleTransfer.resolveTo(
    from: MutableMap<String, List<WithdrawingTransferableResource>>,
    to: MutableMap<String, List<DepositingTransferableResource>>
) {
    from[this.from.addressString()] = listOf(
        WithdrawingTransferableResource(
            resource = transferred.toTransferableResource()
        )
    )
    to[this.to.addressString()] = listOf(
        DepositingTransferableResource(resource = transferred.toTransferableResource())
    )
}

fun TransactionType.Transfer.resolveTo(
    from: MutableMap<String, List<WithdrawingTransferableResource>>,
    to: MutableMap<String, List<DepositingTransferableResource>>
) {
    from[this.from.addressString()] = emptyList()
    to.putAll(
        transfers.mapValues { toTransfers ->
            toTransfers.value.entries.map { resourceEntry ->
                DepositingTransferableResource(resource = resourceEntry.value.toTransferableResource(resourceEntry.key))
            }
        }
    )
}


fun ResourceSpecifier.toTransferableResource(): TransferableResource = when (this) {
    is ResourceSpecifier.Amount -> TransferableResource.Amount(
        Resource.FungibleResource(
            resourceAddress = resourceAddress.addressString(),
            amount = amount.asStr().toBigDecimal()
        )
    )

    is ResourceSpecifier.Ids -> TransferableResource.NFTs(
        items = ids.map { id ->
            Resource.NonFungibleResource.Item(
                collectionAddress = this.resourceAddress.addressString(),
                localId = Resource.NonFungibleResource.Item.ID.from(id.asStr()),
                iconMetadataItem = null,
                nameMetadataItem = null
            )
        }
    )
}

fun Resources.toTransferableResource(resourceAddress: String) = when (this) {
    is Resources.Amount -> TransferableResource.Amount(
        Resource.FungibleResource(
            resourceAddress = resourceAddress,
            amount = amount.asStr().toBigDecimal()
        )
    )

    is Resources.Ids -> TransferableResource.NFTs(
        ids.map { id ->
            Resource.NonFungibleResource.Item(
                collectionAddress = resourceAddress,
                localId = Resource.NonFungibleResource.Item.ID.from(id.asStr()),
                nameMetadataItem = null,
                iconMetadataItem = null
            )
        }
    )
}
