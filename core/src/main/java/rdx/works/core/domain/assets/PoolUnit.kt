package rdx.works.core.domain.assets

import rdx.works.core.divideWithDivisibility
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
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
        val poolUnitSupply = stake.currentSupply ?: BigDecimal.ZERO
        val stakeAmount = stake.ownedAmount
        return if (stakeAmount != null && stake.divisibility != null && poolUnitSupply != BigDecimal.ZERO) {
            stakeAmount
                .multiply(resourceVaultBalance)
                .divideWithDivisibility(poolUnitSupply, stake.divisibility)
        } else {
            null
        }
    }
}
