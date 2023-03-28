package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import java.math.BigDecimal

val FungibleResourcesCollectionItem.amountDecimal: BigDecimal
    get() = when (this) {
        is FungibleResourcesCollectionItemGloballyAggregated -> BigDecimal(amount)
        is FungibleResourcesCollectionItemVaultAggregated ->
            vaults.items.sumOf { item -> BigDecimal(item.amount) }
        else -> BigDecimal.ZERO
    }
