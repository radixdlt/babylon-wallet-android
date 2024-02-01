package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

data class PoolUnit(
    val stake: Resource.FungibleResource,
    val pool: Pool? = null
) : Asset.Fungible {

    override val resource: Resource.FungibleResource
        get() = stake

    val resourceAddress: String
        get() = stake.resourceAddress

    val displayTitle: String
        get() = stake.name.ifEmpty {
            stake.symbol
        }

    fun resourceRedemptionValue(item: Resource.FungibleResource): BigDecimal? {
        val resourceVaultBalance = pool?.resources?.find { it.resourceAddress == item.resourceAddress }?.ownedAmount ?: return null
        return if (
            stake.ownedAmount != null &&
            stake.divisibility != null &&
            stake.currentSupply != null &&
            stake.currentSupply != BigDecimal.ZERO
        ) {
            stake.ownedAmount
                .multiply(resourceVaultBalance)
                .divide(stake.currentSupply, stake.mathContext)
        } else {
            null
        }
    }
}
