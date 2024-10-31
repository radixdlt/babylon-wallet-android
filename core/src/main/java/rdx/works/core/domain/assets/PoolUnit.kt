package rdx.works.core.domain.assets

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.samples.SampleWithRandomValues
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.domain.roundedWith

data class PoolUnit(
    val stake: Resource.FungibleResource,
    val pool: Pool? = null
) : Asset.Fungible {

    override val resource: Resource.FungibleResource
        get() = stake
    val resourceAddress: ResourceAddress
        get() = stake.address

    val displayTitle: String
        get() = stake.name.ifEmpty {
            stake.symbol
        }

    /**
     * Calculates the redemption value for the given item in pool associated with this [address] address.
     *
     * @param [address] of the pool item to calculate the redemption value
     * @param [poolUnitAmount] of the pool unit. Defaults to the pool unit's resource owned amount
     * @param [contributionPerResource] a dictionary of how much each pool item contributes to a pool. Defaults to each pool
     * item's owned amount.
     */
    fun poolItemRedemptionValue(
        address: ResourceAddress,
        poolUnitAmount: Decimal192? = stake.ownedAmount,
        contributionPerResource: Map<ResourceAddress, Decimal192> = pool?.resources?.mapNotNull { entry ->
            if (entry.ownedAmount == null) return@mapNotNull null
            entry.address to entry.ownedAmount
        }?.associate { it.first to it.second }.orEmpty()
    ): Decimal192? {
        if (poolUnitAmount == null) return null
        val poolItemContribution = contributionPerResource[address] ?: return null
        val poolUnitSupply = stake.currentSupply.orZero()
        return if (stake.divisibility != null && !poolUnitSupply.isZero) {
            ((poolUnitAmount * poolItemContribution) / poolUnitSupply).roundedWith(divisibility = stake.divisibility)
        } else {
            null
        }
    }

    companion object {
        @UsesSampleValues
        val sampleMainnet: SampleWithRandomValues<PoolUnit> = object : SampleWithRandomValues<PoolUnit> {
            override fun invoke(): PoolUnit = with(Pool.sampleMainnet()) {
                PoolUnit(
                    stake = Resource.FungibleResource.sampleMainnet().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.POOL.key,
                                        value = address.string,
                                        valueType = MetadataType.Address
                                    )
                                )
                            }
                        )
                    },
                    pool = this
                )
            }

            override fun other(): PoolUnit = with(Pool.sampleMainnet.other()) {
                PoolUnit(
                    stake = Resource.FungibleResource.sampleMainnet.other().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.POOL.key,
                                        value = address.string,
                                        valueType = MetadataType.Address
                                    )
                                )
                            }
                        )
                    },
                    pool = this
                )
            }

            override fun random(): PoolUnit = with(Pool.sampleMainnet.other()) {
                PoolUnit(
                    stake = Resource.FungibleResource.sampleMainnet.other().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.POOL.key,
                                        value = address.string,
                                        valueType = MetadataType.Address
                                    )
                                )
                            }
                        )
                    },
                    pool = this
                )
            }
        }
    }
}
