package rdx.works.core.domain.assets

import android.net.Uri
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.samples.SampleWithRandomValues
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.domain.roundedWith

data class LiquidStakeUnit(
    val fungibleResource: Resource.FungibleResource,
    val validator: Validator
) : Asset.Fungible {

    override val resource: Resource.FungibleResource
        get() = fungibleResource

    val resourceAddress: ResourceAddress
        get() = fungibleResource.address

    val name: String
        get() = fungibleResource.name

    val iconUrl: Uri?
        get() = fungibleResource.iconUrl

    fun stakeValueXRD(
        ownedAmount: Decimal192? = fungibleResource.ownedAmount
    ): Decimal192? {
        val totalXrdStake = validator.totalXrdStake ?: return null
        val percentage = percentageOwned(amount = ownedAmount) ?: return null

        return (percentage * totalXrdStake).roundedWith(divisibility = fungibleResource.divisibility)
    }

    private fun percentageOwned(amount: Decimal192?): Decimal192? {
        val amountOwned = amount ?: return null
        val supply = fungibleResource.currentSupply ?: return null

        return (amountOwned / supply).roundedWith(divisibility = fungibleResource.divisibility)
    }

    companion object {
        @UsesSampleValues
        val sampleMainnet: SampleWithRandomValues<LiquidStakeUnit> = object : SampleWithRandomValues<LiquidStakeUnit> {
            override fun invoke(): LiquidStakeUnit = with(Validator.sampleMainnet()) {
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource.sampleMainnet.random().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(ExplicitMetadataKey.VALIDATOR.key, this@with.address.string, MetadataType.Address)
                                )
                            }
                        )
                    },
                    validator = this
                )
            }

            override fun other(): LiquidStakeUnit = with(Validator.sampleMainnet.other()) {
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource.sampleMainnet.random().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(ExplicitMetadataKey.VALIDATOR.key, this@with.address.string, MetadataType.Address)
                                )
                            }
                        )
                    },
                    validator = this
                )
            }

            override fun random(): LiquidStakeUnit = with(Validator.sampleMainnet.other()) {
                LiquidStakeUnit(
                    fungibleResource = Resource.FungibleResource.sampleMainnet.random().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(ExplicitMetadataKey.VALIDATOR.key, this@with.address.string, MetadataType.Address)
                                )
                            }
                        )
                    },
                    validator = this
                )
            }
        }
    }
}
