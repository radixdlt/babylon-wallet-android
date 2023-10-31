package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

data class PoolUnit(
    val stake: Resource.FungibleResource,
    val pool: Pool? = null,
) {

    val resourceAddress: String
        get() = stake.resourceAddress

    val name: String
        get() = stake.name

    val iconUrl: Uri?
        get() = stake.iconUrl

    fun resourceRedemptionValue(item: Resource.FungibleResource): BigDecimal? {
        val resourceVaultBalance = pool?.resources?.find { it.resourceAddress == item.resourceAddress }?.ownedAmount ?: return null
        return if (stake.ownedAmount != null && stake.divisibility != null && stake.currentSupply != null) {
            stake.ownedAmount
                .multiply(resourceVaultBalance)
                .divide(stake.currentSupply, stake.mathContext)
        } else {
            null
        }
    }
}
