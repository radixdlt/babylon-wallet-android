package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregatedVaultItem
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.sumOf

val FungibleResourcesCollectionItem.amountDecimal: Decimal192
    get() = when (this) {
        is FungibleResourcesCollectionItemGloballyAggregated -> amount.toDecimal192()
        is FungibleResourcesCollectionItemVaultAggregated ->
            vaults.items.sumOf { item -> item.amount.toDecimal192() }
        else -> 0.toDecimal192()
    }

val FungibleResourcesCollectionItem.vaultAddress: String?
    get() = when (this) {
        is FungibleResourcesCollectionItemVaultAggregated -> vaults.items.firstOrNull()?.vaultAddress
        is FungibleResourcesCollectionItemGloballyAggregated -> null
        else -> null
    }

val FungibleResourcesCollectionItemVaultAggregatedVaultItem.amountDecimal: Decimal192
    get() = amount.toDecimal192()
