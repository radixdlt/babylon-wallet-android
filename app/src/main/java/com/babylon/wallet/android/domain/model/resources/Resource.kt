package com.babylon.wallet.android.domain.model.resources

import android.net.Uri
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.assets.AssetBehaviours
import com.babylon.wallet.android.domain.model.resources.XrdResource.addressesPerNetwork
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.PoolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StringMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.TagsMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ValidatorMetadataItem
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.knownAddresses
import com.radixdlt.ret.nonFungibleLocalIdAsStr
import com.radixdlt.ret.nonFungibleLocalIdFromStr
import rdx.works.core.displayableQuantity
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
        val nameMetadataItem: NameMetadataItem? = null,
        val symbolMetadataItem: SymbolMetadataItem? = null,
        val descriptionMetadataItem: DescriptionMetadataItem? = null,
        val iconUrlMetadataItem: IconUrlMetadataItem? = null,
        val tagsMetadataItem: TagsMetadataItem? = null,
        private val assetBehaviours: AssetBehaviours? = null,
        val currentSupply: BigDecimal? = null,
        val validatorMetadataItem: ValidatorMetadataItem? = null,
        val poolMetadataItem: PoolMetadataItem? = null,
        val dAppDefinitionsMetadataItem: DAppDefinitionsMetadataItem? = null,
        val divisibility: Int? = null
    ) : Resource(), Comparable<FungibleResource> {
        override val name: String
            get() = nameMetadataItem?.name.orEmpty()

        val symbol: String
            get() = symbolMetadataItem?.symbol.orEmpty()

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        override val iconUrl: Uri?
            get() = iconUrlMetadataItem?.url

        val validatorAddress: String?
            get() = validatorMetadataItem?.validatorAddress

        val poolAddress: String?
            get() = poolMetadataItem?.poolAddress

        val dappDefinitions: List<String>
            get() = dAppDefinitionsMetadataItem?.addresses.orEmpty()

        val tags: List<Tag>
            get() = if (isXrd) {
                tagsMetadataItem?.tags?.map { Tag.Dynamic(name = it) }?.plus(Tag.Official).orEmpty()
            } else {
                tagsMetadataItem?.tags?.map { Tag.Dynamic(name = it) }.orEmpty()
            }

        val behaviours: AssetBehaviours? = if (assetBehaviours != null && isXrd) {
            assetBehaviours.filterNot { it == AssetBehaviour.INFORMATION_CHANGEABLE }.toSet()
        } else {
            assetBehaviours
        }

        val currentSupplyToDisplay: String?
            get() = currentSupply?.displayableQuantity()

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

            val symbolDiff = when {
                symbolMetadataItem == null && other.symbolMetadataItem != null -> 1
                symbolMetadataItem != null && other.symbolMetadataItem == null -> -1
                symbolMetadataItem != null && other.symbolMetadataItem != null -> symbolMetadataItem.symbol.compareTo(
                    other.symbolMetadataItem.symbol
                )

                else -> when {
                    nameMetadataItem == null && other.nameMetadataItem != null -> 1
                    nameMetadataItem != null && other.nameMetadataItem == null -> -1
                    nameMetadataItem != null && other.nameMetadataItem != null -> nameMetadataItem.name.compareTo(
                        other.nameMetadataItem.name
                    )

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
        val nameMetadataItem: NameMetadataItem? = null,
        val descriptionMetadataItem: DescriptionMetadataItem? = null,
        val iconMetadataItem: IconUrlMetadataItem? = null,
        val tagsMetadataItem: TagsMetadataItem? = null,
        private val assetBehaviours: AssetBehaviours? = null,
        val items: List<Item>,
        val currentSupply: Int? = null,
        val validatorMetadataItem: ValidatorMetadataItem? = null,
        val dAppDefinitionsMetadataItem: DAppDefinitionsMetadataItem? = null,
    ) : Resource(), Comparable<NonFungibleResource> {
        override val name: String
            get() = nameMetadataItem?.name.orEmpty()

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        override val iconUrl: Uri?
            get() = iconMetadataItem?.url

        val tags: List<Tag>
            get() = tagsMetadataItem?.tags?.map { Tag.Dynamic(name = it) }.orEmpty()

        val validatorAddress: String?
            get() = validatorMetadataItem?.validatorAddress

        val dappDefinitions: List<String>
            get() = dAppDefinitionsMetadataItem?.addresses.orEmpty()

        val behaviours: AssetBehaviours? = assetBehaviours

        override fun compareTo(other: NonFungibleResource): Int = when {
            nameMetadataItem == null && other.nameMetadataItem != null -> 1
            nameMetadataItem != null && other.nameMetadataItem == null -> -1
            nameMetadataItem != null && other.nameMetadataItem != null -> nameMetadataItem.name.compareTo(
                other.nameMetadataItem.name
            )

            else -> resourceAddress.compareTo(other.resourceAddress)
        }

        data class Item(
            val collectionAddress: String,
            val localId: ID,
            val nameMetadataItem: NameMetadataItem? = null,
            val iconMetadataItem: IconUrlMetadataItem? = null,
            val claimEpochMetadataItem: ClaimEpochMetadataItem? = null,
            val claimAmountMetadataItem: ClaimAmountMetadataItem? = null,
            val remainingMetadata: List<StringMetadataItem> = emptyList()
        ) : Comparable<Item> {

            val globalAddress: String
                get() = "$collectionAddress:${localId.code}"

            val imageUrl: Uri?
                get() = iconMetadataItem?.url

            val claimAmountXrd: BigDecimal?
                get() = claimAmountMetadataItem?.amount

            fun isReadyToClaim(epoch: Long): Boolean {
                return claimEpochMetadataItem?.claimEpoch != null && claimEpochMetadataItem.claimEpoch >= epoch
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
