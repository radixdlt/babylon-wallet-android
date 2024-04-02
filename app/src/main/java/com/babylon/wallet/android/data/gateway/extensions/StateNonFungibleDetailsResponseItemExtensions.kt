package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValue
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueArray
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueBool
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueBytes
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueDecimal
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueEnum
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI128
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI16
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI32
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI64
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI8
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueMap
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueNonFungibleLocalId
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueOwn
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValuePreciseDecimal
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueReference
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueString
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueTuple
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU128
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU16
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU32
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU64
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU8
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.utils.isValidUrl
import rdx.works.core.AddressHelper
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

private enum class SborTypeName(val code: String) {
    INSTANT("Instant")
}

// https://docs.radixdlt.com/v1/docs/metadata-for-wallet-display#nonfungibles
private val stringNFDataKeys = listOf(
    ExplicitMetadataKey.NAME.key,
    ExplicitMetadataKey.DESCRIPTION.key
)
private val urlNFDataKeys = listOf(
    ExplicitMetadataKey.KEY_IMAGE_URL.key
)

fun StateNonFungibleDetailsResponseItem.toMetadata(): List<Metadata> {
    val fields = (data?.programmaticJson as? ProgrammaticScryptoSborValueTuple)?.fields ?: return emptyList()
    return fields.mapNotNull { it.toMetadata() }
}

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun ProgrammaticScryptoSborValue.toMetadata(isCollection: Boolean = false): Metadata? = fieldName?.let { key ->
    when (val sborValue = this) {
        is ProgrammaticScryptoSborValueString -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = if (!isCollection && key in stringNFDataKeys) {
                // Keep the type as string even if the content resembles another type,
                // when the key is in the list of explicitly handled NFT data
                MetadataType.String
            } else if (!isCollection && key in urlNFDataKeys) {
                MetadataType.Url
            } else {
                if (sborValue.value.isValidUrl()) {
                    MetadataType.Url
                } else if (AddressHelper.isValid(sborValue.value)) {
                    MetadataType.Address
                } else {
                    MetadataType.String
                }
            }
        )

        is ProgrammaticScryptoSborValueDecimal -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Decimal
        )

        is ProgrammaticScryptoSborValuePreciseDecimal -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Decimal
        )

        is ProgrammaticScryptoSborValueBool -> Metadata.Primitive(
            key = key,
            value = sborValue.value.toString(),
            valueType = MetadataType.Bool
        )

        is ProgrammaticScryptoSborValueI8 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.BYTE)
        )

        is ProgrammaticScryptoSborValueI16 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.SHORT)
        )

        is ProgrammaticScryptoSborValueI32 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.INT)
        )

        is ProgrammaticScryptoSborValueI64 ->
            if (sborValue.typeName == SborTypeName.INSTANT.code && sborValue.value.toLongOrNull() != null) {
                Metadata.Primitive(
                    key = key,
                    value = sborValue.value,
                    valueType = MetadataType.Instant
                )
            } else {
                Metadata.Primitive(
                    key = key,
                    value = sborValue.value,
                    valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.LONG)
                )
            }

        is ProgrammaticScryptoSborValueI128 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.BIG_INT)
        )

        is ProgrammaticScryptoSborValueU8 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.BYTE)
        )

        is ProgrammaticScryptoSborValueU16 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.SHORT)
        )

        is ProgrammaticScryptoSborValueU32 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.INT)
        )

        is ProgrammaticScryptoSborValueU64 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
        )

        is ProgrammaticScryptoSborValueU128 -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.BIG_INT)
        )

        is ProgrammaticScryptoSborValueEnum -> Metadata.Primitive(
            key = key,
            value = sborValue.variantName.orEmpty(),
            valueType = MetadataType.Enum
        )

        is ProgrammaticScryptoSborValueBytes -> Metadata.Primitive(
            key = key,
            value = sborValue.hex,
            valueType = MetadataType.Bytes
        )

        is ProgrammaticScryptoSborValueArray -> Metadata.Collection(
            key = key,
            values = sborValue.elements.mapNotNull { it.toMetadata(isCollection = true) }
        )

        is ProgrammaticScryptoSborValueMap -> Metadata.Map(
            key = key,
            values = sborValue.propertyEntries.mapNotNull { entry ->
                val entryKey = entry.key.toMetadata(isCollection = true) ?: return@mapNotNull null
                val entryValue = entry.value.toMetadata(isCollection = true) ?: return@mapNotNull null
                entryKey to entryValue
            }.toMap()
        )

        is ProgrammaticScryptoSborValueTuple -> Metadata.Collection(
            key = key,
            values = sborValue.fields.mapNotNull { it.toMetadata(isCollection = true) }
        )

        is ProgrammaticScryptoSborValueReference -> if (AddressHelper.isValid(sborValue.value)) {
            Metadata.Primitive(
                key = key,
                value = sborValue.value,
                valueType = MetadataType.Address
            )
        } else {
            null
        }

        is ProgrammaticScryptoSborValueOwn -> if (AddressHelper.isValid(sborValue.value)) {
            Metadata.Primitive(
                key = key,
                value = sborValue.value,
                valueType = MetadataType.Address
            )
        } else {
            null
        }

        is ProgrammaticScryptoSborValueNonFungibleLocalId -> Metadata.Primitive(
            key = key,
            value = sborValue.value,
            valueType = MetadataType.NonFungibleLocalId
        )

        else -> null
    }
}
