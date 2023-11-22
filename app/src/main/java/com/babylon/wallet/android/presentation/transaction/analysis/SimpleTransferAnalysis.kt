package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.findFungible
import com.babylon.wallet.android.domain.model.resources.findNonFungible
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.TransactionType
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun TransactionType.SimpleTransfer.resolve(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>
): PreviewType {
    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address == from.addressString() || it.address == to.addressString()
    }

    val transferableResource = transferred.toTransferableResource(resources = resources)
    val ownedFromAccount = allAccounts.find { it.address == from.addressString() }
    val fromAccount = if (ownedFromAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedFromAccount,
            resources = listOf(Transferable.Withdrawing(transferableResource))
        )
    } else {
        AccountWithTransferableResources.Other(
            address = from.addressString(),
            resources = listOf(Transferable.Withdrawing(transferableResource))
        )
    }

    val ownedToAccount = allAccounts.find { it.address == to.addressString() }
    val toAccount = if (ownedToAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedToAccount,
            resources = listOf(Transferable.Depositing(transferableResource))
        )
    } else {
        AccountWithTransferableResources.Other(
            address = to.addressString(),
            resources = listOf(Transferable.Depositing(transferableResource))
        )
    }

    return PreviewType.Transfer(from = listOf(fromAccount), to = listOf(toAccount))
}

private fun ResourceSpecifier.toTransferableResource(
    resources: List<Resource>,
): TransferableResource {
    return when (this) {
        is ResourceSpecifier.Amount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = resources.findFungible(resourceAddress.addressString()) ?: Resource.FungibleResource(
                resourceAddress = resourceAddress.addressString(),
                ownedAmount = null
            ),
            isNewlyCreated = false
        )

        is ResourceSpecifier.Ids -> {
            val items = ids.map { id ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = this.resourceAddress.addressString(),
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }
            TransferableResource.NFTs(
                resource = resources.findNonFungible(resourceAddress.addressString())?.copy(
                    items = items
                ) ?: Resource.NonFungibleResource(
                    resourceAddress = resourceAddress.addressString(),
                    amount = ids.size.toLong(),
                    items = items
                ),
                isNewlyCreated = false
            )
        }
    }
}
