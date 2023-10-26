package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemVaultAggregated

val NonFungibleResourcesCollectionItem.amount: Long
    get() = when (this) {
        is NonFungibleResourcesCollectionItemGloballyAggregated -> amount
        is NonFungibleResourcesCollectionItemVaultAggregated ->
            vaults.items.sumOf { item -> item.totalCount }
        else -> 0L
    }

val NonFungibleResourcesCollectionItem.vaultAddress: String?
    get() = when (this) {
        is NonFungibleResourcesCollectionItemVaultAggregated -> vaults.items.firstOrNull()?.vaultAddress
        is NonFungibleResourcesCollectionItemGloballyAggregated -> null
        else -> null
    }
