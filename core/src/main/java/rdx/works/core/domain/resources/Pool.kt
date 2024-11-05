package rdx.works.core.domain.resources

import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.SampleWithRandomValues
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.name
import kotlin.random.Random

data class Pool(
    val address: PoolAddress,
    val metadata: List<Metadata>,
    val resources: List<Resource.FungibleResource>,
    val associatedDApp: DApp? = null
) {

    val name: String
        get() = metadata.name().orEmpty()

    companion object {
        @UsesSampleValues
        val sampleMainnet: SampleWithRandomValues<Pool> = object : SampleWithRandomValues<Pool> {
            override fun invoke(): Pool = Pool(
                address = PoolAddress.sampleMainnet(),
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Sample Pool 1",
                        valueType = MetadataType.String
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.ICON_URL.key,
                        value = "https://images.theconversation.com/files/439369/original/file-20220104-19-12kg47e.jpg",
                        valueType = MetadataType.Url
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.POOL_UNIT.key,
                        value = ResourceAddress.sampleMainnet.candy.string,
                        valueType = MetadataType.Address
                    ),
                    Metadata.Collection(
                        key = "pool_resources",
                        values = listOf(
                            Metadata.Primitive("pool_resources", ResourceAddress.sampleMainnet.xrd.string, MetadataType.Address),
                            Metadata.Primitive("pool_resources", ResourceAddress.sampleMainnet.candy.string, MetadataType.Address),
                        )
                    )
                ),
                resources = listOf(
                    Resource.FungibleResource.sampleMainnet.random(),
                    Resource.FungibleResource.sampleMainnet.random()
                ),
                associatedDApp = DApp.sampleMainnet()
            )

            override fun other(): Pool = Pool(
                address = PoolAddress.sampleMainnet.other(),
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Sample Pool 2",
                        valueType = MetadataType.String
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.POOL_UNIT.key,
                        value = ResourceAddress.sampleMainnet.candy.string,
                        valueType = MetadataType.Address
                    ),
                    Metadata.Collection(
                        key = "pool_resources",
                        values = listOf(
                            Metadata.Primitive("pool_resources", ResourceAddress.sampleMainnet.xrd.string, MetadataType.Address),
                            Metadata.Primitive("pool_resources", ResourceAddress.sampleMainnet.candy.string, MetadataType.Address),
                        )
                    )
                ),
                resources = listOf(
                    Resource.FungibleResource.sampleMainnet.random(),
                    Resource.FungibleResource.sampleMainnet.random()
                )
            )

            override fun random(): Pool = with(
                listOf(
                    Resource.FungibleResource.sampleMainnet.random(),
                    Resource.FungibleResource.sampleMainnet.random()
                )
            ) {
                Pool(
                    address = PoolAddress.sampleMainnet.random(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.NAME.key,
                            value = "Sample Pool ${Random.nextInt()}",
                            valueType = MetadataType.String
                        ),
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.POOL_UNIT.key,
                            value = ResourceAddress.sampleMainnet.candy.string,
                            valueType = MetadataType.Address
                        ),
                        Metadata.Collection(
                            key = "pool_resources",
                            values = map {
                                Metadata.Primitive("pool_resources", it.address.string, MetadataType.Address)
                            }
                        )
                    ),
                    resources = this
                )
            }
        }
    }
}
