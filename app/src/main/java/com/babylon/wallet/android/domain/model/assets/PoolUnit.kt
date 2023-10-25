package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

data class PoolUnit(
    val pool: Resource.FungibleResource,
    val items: List<Resource.FungibleResource> = emptyList()
) {

    val resourceAddress: String
        get() = pool.resourceAddress

    val name: String
        get() = pool.name

    val iconUrl: Uri?
        get() = pool.iconUrl

    fun resourceRedemptionValue(item: Resource.FungibleResource): BigDecimal? {
        val resourceVaultBalance = items.find { it.resourceAddress == item.resourceAddress }?.ownedAmount ?: return null
        return if (pool.ownedAmount != null && pool.divisibility != null && pool.currentSupply != null) {
            pool.ownedAmount
                .multiply(resourceVaultBalance)
                .divide(pool.currentSupply, pool.mathContext)
        } else {
            null
        }
    }
}
