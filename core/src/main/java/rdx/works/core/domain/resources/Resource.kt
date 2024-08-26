package rdx.works.core.domain.resources

import android.net.Uri
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.intId
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.xrd
import com.radixdlt.sargon.samples.SampleWithRandomValues
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.claimAmount
import rdx.works.core.domain.resources.metadata.claimEpoch
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.infoUrl
import rdx.works.core.domain.resources.metadata.keyImageUrl
import rdx.works.core.domain.resources.metadata.name
import rdx.works.core.domain.resources.metadata.poolAddress
import rdx.works.core.domain.resources.metadata.symbol
import rdx.works.core.domain.resources.metadata.tags
import rdx.works.core.domain.resources.metadata.validatorAddress
import kotlin.random.Random

sealed class Resource {
    abstract val address: ResourceAddress
    abstract val validatorAddress: ValidatorAddress?
    abstract val name: String
    abstract val iconUrl: Uri?
    abstract val metadata: List<Metadata>

    val isDetailsAvailable: Boolean
        get() = when (this) {
            is FungibleResource -> currentSupply != null && divisibility != null && behaviours != null
            is NonFungibleResource -> currentSupply != null && behaviours != null
        }

    val nonStandardMetadata: List<Metadata> by lazy {
        metadata.filterNot { item ->
            item.key in ExplicitMetadataKey.entries.map { it.key }.toSet()
        }
    }

    data class FungibleResource(
        override val address: ResourceAddress,
        val ownedAmount: Decimal192?,
        private val assetBehaviours: AssetBehaviours? = null,
        val currentSupply: Decimal192? = null,
        val divisibility: Divisibility? = null,
        override val metadata: List<Metadata> = emptyList()
    ) : Resource(), Comparable<FungibleResource> {

        override val name: String by lazy {
            metadata.name().orEmpty().truncate(maxNumberOfCharacters = NAME_MAX_CHARS)
        }

        val symbol: String by lazy {
            metadata.symbol().orEmpty()
        }

        val description: String by lazy {
            metadata.description().orEmpty().truncate(maxNumberOfCharacters = DESCRIPTION_MAX_CHARS)
        }

        override val iconUrl: Uri? by lazy {
            metadata.iconUrl()
        }

        val infoUrl: Uri? by lazy {
            metadata.infoUrl()
        }

        override val validatorAddress: ValidatorAddress? by lazy {
            metadata.validatorAddress()
        }

        val poolAddress: PoolAddress? by lazy {
            metadata.poolAddress()
        }

        val isXrd: Boolean by lazy {
            ResourceAddress.xrd(address.networkId) == address
        }

        val tags: ImmutableList<Tag> by lazy {
            if (isXrd) {
                metadata.tags()?.map {
                    Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
                }?.plus(Tag.Official).orEmpty()
            } else {
                metadata.tags()?.map {
                    Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
                }.orEmpty()
            }.take(TAGS_MAX).toImmutableList()
        }

        val behaviours: AssetBehaviours? = if (assetBehaviours != null && isXrd) {
            assetBehaviours.filterNot { it == AssetBehaviour.INFORMATION_CHANGEABLE }.toSet()
        } else {
            assetBehaviours
        }

        @Suppress("CyclomaticComplexMethod")
        override fun compareTo(other: FungibleResource): Int {
            // XRD should always be first
            if (isXrd) {
                return -1
            } else if (other.isXrd) {
                return 1
            }

            val symbol = metadata.symbol()
            val otherSymbol = other.metadata.symbol()

            val name = metadata.name()
            val otherName = other.metadata.name()

            val symbolDiff = when {
                symbol == null && otherSymbol != null -> 1
                symbol != null && otherSymbol == null -> -1
                symbol != null && otherSymbol != null -> symbol.compareTo(otherSymbol)

                else -> when {
                    name == null && otherName != null -> 1
                    name != null && otherName == null -> -1
                    name != null && otherName != null -> name.compareTo(otherName)

                    else -> 0
                }
            }

            return if (symbolDiff != 0) {
                symbolDiff
            } else {
                address.string.compareTo(other.address.string)
            }
        }

        companion object
    }

    data class NonFungibleResource(
        override val address: ResourceAddress,
        val amount: Long,
        private val assetBehaviours: AssetBehaviours? = null,
        val items: List<Item>,
        val currentSupply: Int? = null,
        override val metadata: List<Metadata> = emptyList(),
    ) : Resource(), Comparable<NonFungibleResource> {
        override val name: String by lazy {
            metadata.name().orEmpty().truncate(maxNumberOfCharacters = NAME_MAX_CHARS)
        }

        val description: String by lazy {
            metadata.description().orEmpty().truncate(maxNumberOfCharacters = DESCRIPTION_MAX_CHARS)
        }

        override val iconUrl: Uri? by lazy {
            metadata.iconUrl()
        }

        val infoUrl: Uri? by lazy {
            metadata.infoUrl()
        }

        val tags: ImmutableList<Tag> by lazy {
            metadata.tags().orEmpty().map {
                Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
            }.take(TAGS_MAX).toImmutableList()
        }

        override val validatorAddress: ValidatorAddress? by lazy {
            metadata.validatorAddress()
        }

        val behaviours: AssetBehaviours? = assetBehaviours

        override fun compareTo(other: NonFungibleResource): Int {
            val name = metadata.name()
            val otherName = other.metadata.name()
            return when {
                name == null && otherName != null -> 1
                name != null && otherName == null -> -1
                name != null && otherName != null -> name.compareTo(otherName)
                else -> address.string.compareTo(other.address.string)
            }
        }

        data class Item(
            val collectionAddress: ResourceAddress,
            val localId: NonFungibleLocalId,
            val metadata: List<Metadata> = emptyList()
        ) : Comparable<Item> {

            val globalId: NonFungibleGlobalId by lazy {
                NonFungibleGlobalId(
                    resourceAddress = collectionAddress,
                    nonFungibleLocalId = localId
                )
            }

            val name: String? by lazy {
                metadata.name()
            }

            val nameTruncated: String? by lazy {
                name?.truncate(maxNumberOfCharacters = NAME_MAX_CHARS)
            }

            val description: String? by lazy {
                metadata.description()?.truncate(maxNumberOfCharacters = DESCRIPTION_MAX_CHARS)
            }

            val imageUrl: Uri? by lazy {
                metadata.keyImageUrl()
            }

            val claimAmountXrd: Decimal192? by lazy {
                metadata.claimAmount()
            }

            val claimEpoch: Long? by lazy {
                metadata.claimEpoch()
            }

            val nonStandardMetadata: List<Metadata> by lazy {
                metadata.filterNot { metadataItem ->
                    metadataItem.key in setOf(
                        ExplicitMetadataKey.NAME,
                        ExplicitMetadataKey.DESCRIPTION,
                        ExplicitMetadataKey.KEY_IMAGE_URL,
                        ExplicitMetadataKey.CLAIM_AMOUNT,
                        ExplicitMetadataKey.CLAIM_EPOCH
                    ).map { it.key }
                }
            }

            fun isReadyToClaim(currentEpoch: Long): Boolean {
                return claimEpoch?.let { it <= currentEpoch } ?: false
            }

            override fun compareTo(other: Item): Int = when (localId) {
                is NonFungibleLocalId.Str -> {
                    val otherStr = other.localId as? NonFungibleLocalId.Str

                    if (otherStr != null) {
                        localId.string.compareTo(otherStr.string)
                    } else {
                        -1
                    }
                }

                is NonFungibleLocalId.Ruid -> {
                    val otherRuid = other.localId as? NonFungibleLocalId.Ruid

                    if (otherRuid != null) {
                        localId.string.compareTo(otherRuid.string)
                    } else {
                        -1
                    }
                }

                is NonFungibleLocalId.Bytes -> {
                    val otherBytes = other.localId as? NonFungibleLocalId.Bytes

                    if (otherBytes != null) {
                        localId.string.compareTo(otherBytes.string)
                    } else {
                        -1
                    }
                }

                is NonFungibleLocalId.Integer -> {
                    val otherInteger = (other.localId as? NonFungibleLocalId.Integer)

                    if (otherInteger != null) {
                        localId.value.compareTo(otherInteger.value)
                    } else {
                        -1
                    }
                }
            }
        }

        companion object
    }

    companion object {
        private const val NAME_MAX_CHARS = 32
        private const val DESCRIPTION_MAX_CHARS = 256
        private const val TAG_MAX_CHARS = 16
        private const val TAGS_MAX = 100
    }
}

private fun String.truncate(maxNumberOfCharacters: Int, addEllipsis: Boolean = true): String {
    val ellipsis = if (addEllipsis && length > maxNumberOfCharacters) "…" else ""
    return take(maxNumberOfCharacters) + ellipsis
}

object XrdResource {
    const val SYMBOL = "XRD"

    fun address(networkId: com.radixdlt.sargon.NetworkId): ResourceAddress {
        return ResourceAddress.xrd(networkId)
    }

    fun addressesPerNetwork(): Map<com.radixdlt.sargon.NetworkId, ResourceAddress> =
        com.radixdlt.sargon.NetworkId.entries.associateWith { entry -> address(networkId = entry) }
}

@Suppress("MagicNumber")
@UsesSampleValues
val Resource.FungibleResource.Companion.sampleMainnet
    get() = object : SampleWithRandomValues<Resource.FungibleResource> {
        override fun invoke(): Resource.FungibleResource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.xrd,
            ownedAmount = Decimal192.sample(),
            metadata = listOf(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.NAME.key,
                    value = "Radix",
                    valueType = MetadataType.String
                ),
                Metadata.Primitive(
                    key = ExplicitMetadataKey.SYMBOL.key,
                    value = "XRD",
                    valueType = MetadataType.String
                )
            )
        )

        override fun other(): Resource.FungibleResource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.candy,
            ownedAmount = Decimal192.sample.other(),
            metadata = listOf(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.NAME.key,
                    value = "Candy",
                    valueType = MetadataType.String
                ),
                Metadata.Primitive(
                    key = ExplicitMetadataKey.SYMBOL.key,
                    value = "CND",
                    valueType = MetadataType.String
                )
            )
        )

        override fun random(): Resource.FungibleResource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.random(),
            ownedAmount = Random.nextDouble().toDecimal192(),
            metadata = with(Random.nextInt()) {
                listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Random $this resource",
                        valueType = MetadataType.String
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.SYMBOL.key,
                        value = "RND$this",
                        valueType = MetadataType.String
                    )
                )
            }
        )
    }

@UsesSampleValues
val Resource.NonFungibleResource.Companion.sampleMainnet
    get() = object : SampleWithRandomValues<Resource.NonFungibleResource> {
        override fun invoke(): Resource.NonFungibleResource = Resource.NonFungibleResource(
            address = ResourceAddress.sampleMainnet.nonFungibleGCMembership,
            amount = 2,
            metadata = listOf(
                Metadata.Primitive(
                    key = ExplicitMetadataKey.NAME.key,
                    value = "Collection 1",
                    valueType = MetadataType.String
                )
            ),
            items = listOf(
                Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.nonFungibleGCMembership,
                    localId = NonFungibleLocalId.intId(0.toULong())
                ),
                Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.nonFungibleGCMembership,
                    localId = NonFungibleLocalId.intId(1.toULong())
                )
            )
        )

        override fun other(): Resource.NonFungibleResource = with(ResourceAddress.sampleMainnet.random()) {
            Resource.NonFungibleResource(
                address = this,
                amount = 1,
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Collection 2",
                        valueType = MetadataType.String
                    )
                ),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = this,
                        localId = NonFungibleLocalId.intId(0.toULong())
                    )
                )
            )
        }

        override fun random(): Resource.NonFungibleResource = with(ResourceAddress.sampleMainnet.random()) {
            Resource.NonFungibleResource(
                address = this,
                amount = 1,
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Collection ${Random.nextInt()}",
                        valueType = MetadataType.String
                    )
                ),
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = this,
                        localId = NonFungibleLocalId.intId(0.toULong())
                    )
                )
            )
        }
    }
