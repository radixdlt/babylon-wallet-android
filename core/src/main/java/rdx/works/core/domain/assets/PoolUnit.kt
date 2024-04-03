package rdx.works.core.domain.assets

import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.SampleWithRandomValues
import rdx.works.core.divideWithDivisibility
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.sampleMainnet
import java.math.BigDecimal

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

    fun resourceRedemptionValue(item: Resource.FungibleResource): BigDecimal? {
        val resourceVaultBalance = pool?.resources?.find { it.address == item.address }?.ownedAmount ?: return null
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
