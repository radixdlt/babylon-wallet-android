package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import rdx.works.core.divideWithDivisibility
import java.math.BigDecimal

data class PoolUnit(
    val stake: Resource.FungibleResource,
    val pool: Pool? = null
) : Asset.Fungible {

    override val resource: Resource.FungibleResource
        get() = stake
    override val assetOrder: Int
        get() = 4

    val resourceAddress: String
        get() = stake.resourceAddress

    val displayTitle: String
        get() = stake.name.ifEmpty {
            stake.symbol
        }

    fun resourceRedemptionValue(item: Resource.FungibleResource): BigDecimal? {
        val resourceVaultBalance = pool?.resources?.find { it.resourceAddress == item.resourceAddress }?.ownedAmount ?: return null
        val poolUnitSupply = stake.currentSupply ?: BigDecimal.ZERO
        return if (stake.ownedAmount != null && stake.divisibility != null && poolUnitSupply != BigDecimal.ZERO) {
            stake.ownedAmount
                .multiply(resourceVaultBalance)
                .divideWithDivisibility(poolUnitSupply, stake.divisibility)
        } else {
            null
        }
    }
}
