package rdx.works.core.domain.resources

import android.net.Uri
import com.radixdlt.derivation.model.NetworkId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.AddressHelper
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.claimAmount
import rdx.works.core.domain.resources.metadata.claimEpoch
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.keyImageUrl
import rdx.works.core.domain.resources.metadata.name
import rdx.works.core.domain.resources.metadata.poolAddress
import rdx.works.core.domain.resources.metadata.symbol
import rdx.works.core.domain.resources.metadata.tags
import rdx.works.core.domain.resources.metadata.validatorAddress
import java.math.BigDecimal

sealed class Resource {
    abstract val resourceAddress: String
    abstract val validatorAddress: String?
    abstract val name: String
    abstract val iconUrl: Uri?
    abstract val metadata: List<Metadata>

    val isDetailsAvailable: Boolean
        get() = when (this) {
            is FungibleResource -> currentSupply != null && divisibility != null && behaviours != null
            is NonFungibleResource -> currentSupply != null && behaviours != null
        }

    data class FungibleResource(
        override val resourceAddress: String,
        val ownedAmount: BigDecimal?,
        private val assetBehaviours: AssetBehaviours? = null,
        val currentSupply: BigDecimal? = null,
        val divisibility: Int? = null,
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

        override val validatorAddress: String? by lazy {
            metadata.validatorAddress()
        }

        val poolAddress: String? by lazy {
            metadata.poolAddress()
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

        val displayTitle: String
            get() = if (symbol.isNotBlank()) {
                symbol
            } else if (name.isNotBlank()) {
                name
            } else {
                ""
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
                resourceAddress.compareTo(other.resourceAddress)
            }
        }

        companion object
    }

    data class NonFungibleResource(
        override val resourceAddress: String,
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

        val tags: ImmutableList<Tag> by lazy {
            metadata.tags().orEmpty().map {
                Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
            }.take(TAGS_MAX).toImmutableList()
        }

        override val validatorAddress: String? by lazy {
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
                else -> resourceAddress.compareTo(other.resourceAddress)
            }
        }

        data class Item(
            val collectionAddress: String,
            val localId: ID,
            val metadata: List<Metadata> = emptyList()
        ) : Comparable<Item> {

            val globalAddress: String
                get() = "$collectionAddress:${localId.code}"

            val name: String? by lazy {
                metadata.name()?.truncate(maxNumberOfCharacters = NAME_MAX_CHARS)
            }

            val description: String? by lazy {
                metadata.description()?.truncate(maxNumberOfCharacters = DESCRIPTION_MAX_CHARS)
            }

            val imageUrl: Uri? by lazy {
                metadata.keyImageUrl()
            }

            val claimAmountXrd: BigDecimal? by lazy {
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
                is ID.StringType -> (other.localId as? ID.StringType)?.compareTo(localId) ?: -1
                is ID.IntegerType -> (other.localId as? ID.IntegerType)?.compareTo(localId) ?: -1
                is ID.BytesType -> (other.localId as? ID.BytesType)?.compareTo(localId) ?: -1
                is ID.RUIDType -> (other.localId as? ID.RUIDType)?.compareTo(localId) ?: -1
            }

            sealed class ID {
                abstract val prefix: String
                abstract val suffix: String

                abstract val displayable: String

                val code: String
                    get() = "$prefix$displayable$suffix"

                data class StringType(
                    private val id: String
                ) : ID(), Comparable<StringType> {
                    override val prefix: String = PREFIX
                    override val suffix: String = SUFFIX

                    override val displayable: String
                        get() = id

                    override fun compareTo(other: StringType): Int = other.id.compareTo(id)

                    companion object {
                        const val PREFIX = "<"
                        const val SUFFIX = ">"
                    }
                }

                data class IntegerType(
                    private val id: ULong
                ) : ID(), Comparable<IntegerType> {
                    override val prefix: String = PREFIX
                    override val suffix: String = SUFFIX

                    override val displayable: String
                        get() = id.toString()

                    override fun compareTo(other: IntegerType): Int = other.id.compareTo(id)

                    companion object {
                        const val PREFIX = "#"
                        const val SUFFIX = "#"
                    }
                }

                data class BytesType(
                    private val id: String
                ) : ID(), Comparable<BytesType> {
                    override val prefix: String = PREFIX
                    override val suffix: String = SUFFIX
                    override val displayable: String
                        get() = id

                    override fun compareTo(other: BytesType): Int = other.id.compareTo(id)

                    companion object {
                        const val PREFIX = "["
                        const val SUFFIX = "]"
                    }
                }

                data class RUIDType(
                    private val id: String
                ) : ID(), Comparable<RUIDType> {
                    override val prefix: String = PREFIX
                    override val suffix: String = SUFFIX
                    override val displayable: String
                        get() = id

                    override fun compareTo(other: RUIDType): Int = other.id.compareTo(id)

                    companion object {
                        const val PREFIX = "{"
                        const val SUFFIX = "}"
                    }
                }

                companion object {
                    /**
                     * Infers the type of the [Item].[ID] from its surrounding delimiter
                     *
                     * More info https://docs-babylon.radixdlt.com/main/reference-materials/resource-addressing.html
                     * #_non_fungibles_individual_units_of_non_fungible_resources
                     */
                    fun from(value: String): ID = AddressHelper.localId(value)
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

    fun address(networkId: Int): String {
        return AddressHelper.xrdAddress(forNetworkId = networkId)
    }

    fun addressesPerNetwork(): Map<Int, String> = NetworkId.entries.associate { entry ->
        entry.value to address(networkId = entry.value)
    }
}

val Resource.FungibleResource.isXrd: Boolean
    get() {
        val networkIdValue = AddressHelper.networkIdOrNull(resourceAddress) ?: return false

        return XrdResource.address(networkId = networkIdValue) == resourceAddress
    }
