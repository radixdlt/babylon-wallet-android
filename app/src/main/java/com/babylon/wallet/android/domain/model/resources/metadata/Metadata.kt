package com.babylon.wallet.android.domain.model.resources.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Metadata {
    val key: String
    val lastUpdatedAtStateVersion: Long

    @Serializable
    @SerialName("primitive")
    data class Primitive(
        override val key: String,
        val value: String,
        @SerialName("value_type")
        val valueType: MetadataType,
        override val lastUpdatedAtStateVersion: Long = 0,
    ) : Metadata

    @Serializable
    @SerialName("collection")
    data class Collection(
        override val key: String,
        val values: List<Metadata>,
        override val lastUpdatedAtStateVersion: Long = 0,
    ) : Metadata

    @Serializable
    @SerialName("map")
    data class Map(
        override val key: String,
        val values: kotlin.collections.Map<Metadata, Metadata>,
        override val lastUpdatedAtStateVersion: Long = 0L
    ) : Metadata
}

@Serializable
sealed interface MetadataType {

    @Serializable
    @SerialName("bool")
    data object Bool : MetadataType

    @Serializable
    @SerialName("integer")
    data class Integer(
        val signed: Boolean,
        val size: Size
    ) : MetadataType {
        @Serializable
        enum class Size {
            BYTE, // 8-bit
            SHORT, // 16-bit
            INT, // 32-bit
            LONG, // 64-bit
            BIG_INT // 128-bit
        }
    }

    @Serializable
    @SerialName("string")
    data object String : MetadataType

    @Serializable
    @SerialName("decimal")
    data object Decimal : MetadataType

    @Serializable
    @SerialName("bytes")
    data object Bytes : MetadataType

    @Serializable
    @SerialName("enum")
    data object Enum : MetadataType

    @Serializable
    @SerialName("address")
    data object Address : MetadataType

    @Serializable
    @SerialName("non_fungible_local_id")
    data object NonFungibleLocalId : MetadataType

    @Serializable
    @SerialName("non_fungible_global_id")
    data object NonFungibleGlobalId : MetadataType

    @Serializable
    @SerialName("public_key_ecdsa_secp256k1")
    data object PublicKeyEcdsaSecp256k1 : MetadataType

    @Serializable
    @SerialName("public_key_eddsa_ed25519")
    data object PublicKeyEddsaEd25519 : MetadataType

    @Serializable
    @SerialName("public_key_hash_ecdsa_secp256k1")
    data object PublicKeyHashEcdsaSecp256k1 : MetadataType

    @Serializable
    @SerialName("public_key_hash_eddsa_ed25519")
    data object PublicKeyHashEddsaEd25519 : MetadataType

    @Serializable
    @SerialName("instant")
    data object Instant : MetadataType

    @Serializable
    @SerialName("url")
    data object Url : MetadataType
}
