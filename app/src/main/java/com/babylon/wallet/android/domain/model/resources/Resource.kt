package com.babylon.wallet.android.domain.model.resources

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.assets.AssetBehaviours
import com.babylon.wallet.android.domain.model.resources.XrdResource.addressesPerNetwork
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.claimAmount
import com.babylon.wallet.android.domain.model.resources.metadata.claimEpoch
import com.babylon.wallet.android.domain.model.resources.metadata.dAppDefinitions
import com.babylon.wallet.android.domain.model.resources.metadata.description
import com.babylon.wallet.android.domain.model.resources.metadata.iconUrl
import com.babylon.wallet.android.domain.model.resources.metadata.keyImageUrl
import com.babylon.wallet.android.domain.model.resources.metadata.name
import com.babylon.wallet.android.domain.model.resources.metadata.poolAddress
import com.babylon.wallet.android.domain.model.resources.metadata.symbol
import com.babylon.wallet.android.domain.model.resources.metadata.tags
import com.babylon.wallet.android.domain.model.resources.metadata.validatorAddress
import com.babylon.wallet.android.utils.truncate
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.knownAddresses
import com.radixdlt.ret.nonFungibleLocalIdAsStr
import com.radixdlt.ret.nonFungibleLocalIdFromStr
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.derivation.model.NetworkId
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

sealed class Resource {
    abstract val resourceAddress: String
    abstract val name: String
    abstract val iconUrl: Uri?

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
        val metadata: List<Metadata> = emptyList()
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

        val validatorAddress: String? by lazy {
            metadata.validatorAddress()
        }

        val poolAddress: String? by lazy {
            metadata.poolAddress()
        }

        val dappDefinitions: List<String> by lazy {
            metadata.dAppDefinitions()
        }

        val tags: List<Tag> by lazy {
            if (isXrd) {
                metadata.tags()?.map {
                    Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
                }?.plus(Tag.Official).orEmpty()
            } else {
                metadata.tags()?.map {
                    Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
                }.orEmpty()
            }.take(TAGS_MAX)
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

        val mathContext: MathContext
            get() = if (divisibility == null) {
                MathContext.UNLIMITED
            } else {
                MathContext(divisibility, RoundingMode.HALF_DOWN)
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
        val metadata: List<Metadata> = emptyList(),
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

        val tags: List<Tag> by lazy {
            metadata.tags().orEmpty().map {
                Tag.Dynamic(name = it.truncate(maxNumberOfCharacters = TAG_MAX_CHARS))
            }.take(TAGS_MAX)
        }

        val validatorAddress: String? by lazy {
            metadata.validatorAddress()
        }

        val dappDefinitions: List<String> by lazy {
            metadata.dAppDefinitions()
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

                abstract fun toRetId(): NonFungibleLocalId

                val code: String
                    get() = "$prefix$displayable$suffix"

                data class StringType(
                    private val id: String
                ) : ID(), Comparable<StringType> {
                    override val prefix: String = STRING_PREFIX
                    override val suffix: String = STRING_SUFFIX

                    override val displayable: String
                        get() = id

                    override fun toRetId(): NonFungibleLocalId = NonFungibleLocalId.Str(id)

                    override fun compareTo(other: StringType): Int = other.id.compareTo(id)
                }

                data class IntegerType(
                    private val id: ULong
                ) : ID(), Comparable<IntegerType> {
                    override val prefix: String = INT_DELIMITER
                    override val suffix: String = prefix

                    override val displayable: String
                        get() = id.toString()

                    override fun toRetId(): NonFungibleLocalId = NonFungibleLocalId.Integer(id)

                    override fun compareTo(other: IntegerType): Int = other.id.compareTo(id)
                }

                data class BytesType(
                    private val id: String
                ) : ID(), Comparable<BytesType> {
                    override val prefix: String = BYTES_PREFIX
                    override val suffix: String = BYTES_SUFFIX
                    override val displayable: String
                        get() = id

                    override fun toRetId(): NonFungibleLocalId = nonFungibleLocalIdFromStr("$prefix$id$suffix")

                    override fun compareTo(other: BytesType): Int = other.id.compareTo(id)
                }

                data class RUIDType(
                    private val id: String
                ) : ID(), Comparable<RUIDType> {
                    override val prefix: String = RUID_PREFIX
                    override val suffix: String = RUID_SUFFIX
                    override val displayable: String
                        get() = id

                    override fun toRetId(): NonFungibleLocalId = nonFungibleLocalIdFromStr("$prefix$id$suffix")

                    override fun compareTo(other: RUIDType): Int = other.id.compareTo(id)
                }

                companion object {
                    private const val STRING_PREFIX = "<"
                    private const val STRING_SUFFIX = ">"
                    private const val INT_DELIMITER = "#"
                    private const val BYTES_PREFIX = "["
                    private const val BYTES_SUFFIX = "]"
                    private const val RUID_PREFIX = "{"
                    private const val RUID_SUFFIX = "}"

                    /**
                     * Infers the type of the [Item].[ID] from its surrounding delimiter
                     *
                     * More info https://docs-babylon.radixdlt.com/main/reference-materials/resource-addressing.html
                     * #_non_fungibles_individual_units_of_non_fungible_resources
                     */
                    fun from(value: String): ID = when (val id = nonFungibleLocalIdFromStr(value)) {
                        is NonFungibleLocalId.Integer -> IntegerType(id = id.value)
                        is NonFungibleLocalId.Str -> StringType(id = id.value)
                        is NonFungibleLocalId.Bytes -> BytesType(id = value.removeSurrounding(BYTES_PREFIX, BYTES_SUFFIX))
                        is NonFungibleLocalId.Ruid -> RUIDType(id = value.removeSurrounding(RUID_PREFIX, RUID_SUFFIX))
                    }

                    fun from(id: NonFungibleLocalId): ID = when (id) {
                        is NonFungibleLocalId.Integer -> IntegerType(id = id.value)
                        is NonFungibleLocalId.Str -> StringType(id = id.value)
                        is NonFungibleLocalId.Bytes -> BytesType(
                            id = nonFungibleLocalIdAsStr(id).removeSurrounding(BYTES_PREFIX, BYTES_SUFFIX)
                        )

                        is NonFungibleLocalId.Ruid -> RUIDType(
                            id = nonFungibleLocalIdAsStr(id).removeSurrounding(RUID_PREFIX, RUID_SUFFIX)
                        )
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

object XrdResource {
    const val SYMBOL = "XRD"

    val addressesPerNetwork: Map<NetworkId, String> by lazy {
        NetworkId.values().associateWith { id ->
            knownAddresses(networkId = id.value.toUByte()).resourceAddresses.xrd.addressString()
        }
    }

    fun address(networkId: NetworkId = Radix.Gateway.default.network.networkId()) = addressesPerNetwork[networkId].orEmpty()
}

val Resource.FungibleResource.isXrd: Boolean
    get() = addressesPerNetwork.containsValue(resourceAddress)

fun List<Resource>.findFungible(address: String) = find { it.resourceAddress == address } as? Resource.FungibleResource
fun List<Resource>.findNonFungible(address: String) = find { it.resourceAddress == address } as? Resource.NonFungibleResource
