package com.babylon.wallet.android.domain.model.resources.metadata

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The common denominator for all metadata items, either
 * known ones, or new ones defined by dApp developers.
 */
interface MetadataItem {
    val key: String

    companion object {
        inline fun <reified T : MetadataItem> MutableList<MetadataItem>.consume(): T? {
            val item = find { it is T } as? T
            if (item != null) {
                remove(item)
            }

            return item
        }
    }
}

/**
 * Generic [MetadataItem] that the [key] is not included in the
 * [ExplicitMetadataKey]s and whose [value] can be represented as a [String]
 */
data class StringMetadataItem(
    override val key: String,
    val value: String
) : MetadataItem

/**
 * More complex types as Maps or Tuples that are previewed as complex.
 */
data class ComplexMetadataItem(
    override val key: String
) : MetadataItem

@Serializable
sealed interface Metadata {
    val key: String

    @Serializable
    @SerialName("primitive")
    data class Primitive(
        override val key: String,
        val value: String,
        @SerialName("value_type")
        val valueType: MetadataType
    ): Metadata

    @Serializable
    @SerialName("collection")
    data class Collection(
        override val key: String,
        val values: List<Metadata>,
    ): Metadata

    @Serializable
    @SerialName("map")
    data class Map(
        override val key: String,
        val values: kotlin.collections.Map<Metadata, Metadata>
    ): Metadata
}

@Serializable
sealed interface MetadataType {

    @Serializable
    @SerialName("bool")
    data object Bool: MetadataType
    @Serializable
    @SerialName("integer")
    data class Integer(
        val signed: Boolean,
        val size: Size
    ): MetadataType {
        @Serializable
        enum class Size {
            BYTE,   // 8-bit
            SHORT,  // 16-bit
            INT,    // 32-bit
            LONG,   // 64-bit
            BIG_INT // 128-bit
        }
    }
    @Serializable
    @SerialName("string")
    data object String: MetadataType
    @Serializable
    @SerialName("decimal")
    data object Decimal: MetadataType
    @Serializable
    @SerialName("bytes")
    data object Bytes: MetadataType
    @Serializable
    @SerialName("enum")
    data object Enum: MetadataType
    @Serializable
    @SerialName("address")
    data object Address: MetadataType
    @Serializable
    @SerialName("non_fungible_local_id")
    data object NonFungibleLocalId: MetadataType
    @Serializable
    @SerialName("non_fungible_global_id")
    data object NonFungibleGlobalId: MetadataType
    @Serializable
    @SerialName("public_key_ecdsa_secp256k1")
    data object PublicKeyEcdsaSecp256k1: MetadataType
    @Serializable
    @SerialName("public_key_eddsa_ed25519")
    data object PublicKeyEddsaEd25519: MetadataType
    @Serializable
    @SerialName("public_key_hash_ecdsa_secp256k1")
    data object PublicKeyHashEcdsaSecp256k1: MetadataType
    @Serializable
    @SerialName("public_key_hash_eddsa_ed25519")
    data object PublicKeyHashEddsaEd25519: MetadataType
    @Serializable
    @SerialName("instant")
    data object Instant: MetadataType
    @Serializable
    @SerialName("url")
    data object Url: MetadataType
}
