package rdx.works.core.domain.assets

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.Sample
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.sampleMainnet
import kotlin.random.Random

data class StakeClaim(
    val nonFungibleResource: Resource.NonFungibleResource,
    val validator: Validator
) : Asset.NonFungible {

    override val resource: Resource.NonFungibleResource
        get() = nonFungibleResource
    val validatorAddress: ValidatorAddress
        get() = validator.address

    val resourceAddress: ResourceAddress
        get() = nonFungibleResource.address

    val name: String
        get() = nonFungibleResource.name

    val iconUrl: Uri?
        get() = nonFungibleResource.iconUrl

    fun unstakingNFTs(epoch: Long) = nonFungibleResource.items.filter { !it.isReadyToClaim(epoch) }
    fun readyToClaimNFTs(epoch: Long) = nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }

    companion object {
        @VisibleForTesting
        val sampleMainnet: Sample<StakeClaim> = object : Sample<StakeClaim> {
            override fun invoke(): StakeClaim = with(Validator.sampleMainnet()) {
                StakeClaim(
                    nonFungibleResource = Resource.NonFungibleResource.sampleMainnet.random().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.VALIDATOR.key,
                                        value = this@with.address.string,
                                        valueType = MetadataType.Address
                                    )
                                )
                            },
                            items = it.items.map { item ->
                                item.copy(
                                    metadata = item.metadata.toMutableList().apply {
                                        add(
                                            Metadata.Primitive(
                                                key = ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                                value = Random.nextInt().toString(),
                                                valueType = MetadataType.Decimal
                                            )
                                        )
                                    }
                                )
                            }
                        )
                    },
                    validator = this
                )
            }

            override fun other(): StakeClaim = with(Validator.sampleMainnet.other()) {
                StakeClaim(
                    nonFungibleResource = Resource.NonFungibleResource.sampleMainnet.random().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.VALIDATOR.key,
                                        value = this@with.address.string,
                                        MetadataType.Address
                                    )
                                )
                            },
                            items = it.items.map { item ->
                                item.copy(
                                    metadata = item.metadata.toMutableList().apply {
                                        add(
                                            Metadata.Primitive(
                                                key = ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                                value = Random.nextInt().toString(),
                                                valueType = MetadataType.Decimal
                                            )
                                        )
                                    }
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
