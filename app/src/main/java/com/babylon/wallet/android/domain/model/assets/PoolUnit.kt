package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

data class PoolUnit(
    val poolUnitResource: Resource.FungibleResource,
    val poolResources: List<Resource.FungibleResource> = emptyList()
) {

    val resourceAddress: String
        get() = poolUnitResource.resourceAddress

    val name: String
        get() = poolUnitResource.name

    val iconUrl: Uri?
        get() = poolUnitResource.iconUrl

    fun resourceRedemptionValue(resourceAddress: String): BigDecimal? {
        val resourceVaultBalance = poolResources.find { it.resourceAddress == resourceAddress }?.ownedAmount
        return poolUnitResource.ownedAmount?.multiply(resourceVaultBalance)
            ?.divide(poolUnitResource.currentSupply, poolUnitResource.mathContext)
    }
}
