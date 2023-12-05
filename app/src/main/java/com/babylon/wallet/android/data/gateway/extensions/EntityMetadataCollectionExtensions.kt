package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataItem
import com.babylon.wallet.android.data.gateway.generated.models.MetadataBoolArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataBoolValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataDecimalArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataDecimalValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataGlobalAddressArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataGlobalAddressValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataI32ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataI32Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataI64ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataI64Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataInstantArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataInstantValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataNonFungibleGlobalIdArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataNonFungibleGlobalIdValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataNonFungibleLocalIdArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataNonFungibleLocalIdValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataOriginArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataOriginValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataPublicKeyArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataPublicKeyHashArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataPublicKeyHashValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataPublicKeyValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataStringArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataStringValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU32ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU32Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU64ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU64Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU8ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU8Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlValue
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEcdsaSecp256k1
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyEddsaEd25519
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEcdsaSecp256k1
import com.babylon.wallet.android.data.gateway.generated.models.PublicKeyHashEddsaEd25519
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType.Integer.Size

fun EntityMetadataCollection.toMetadata(): List<Metadata> {
    return items.mapNotNull { item -> item.toMetadata() }
}

@Suppress("CyclomaticComplexMethod", "LongMethod")
fun EntityMetadataItem.toMetadata(): Metadata? = when (val typed = value.typed) {
    is MetadataBoolValue -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Bool
    )

    is MetadataBoolArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Bool
            )
        }
    )

    is MetadataDecimalValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Decimal
    )

    is MetadataDecimalArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Decimal
            )
        }
    )

    is MetadataGlobalAddressValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Address
    )

    is MetadataGlobalAddressArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Address
            )
        },
    )

    is MetadataI32Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = true, size = Size.INT)
    )

    is MetadataI32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = true, size = Size.INT)
            )
        }
    )

    is MetadataI64Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = true, size = Size.LONG)
    )

    is MetadataI64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = true, size = Size.LONG)
            )
        }
    )

    is MetadataU8Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.INT)
    )

    is MetadataU8ArrayValue -> Metadata.Primitive(
        key = key,
        value = typed.valueHex,
        valueType = MetadataType.Bytes
    )

    is MetadataU32Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.INT)
    )

    is MetadataU32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = false, size = Size.INT)
            )
        }
    )

    is MetadataU64Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.LONG)
    )

    is MetadataU64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = false, size = Size.LONG)
            )
        },
    )

    is MetadataInstantValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Instant
    )

    is MetadataInstantArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Instant
            )
        }
    )

    is MetadataNonFungibleGlobalIdValue -> Metadata.Primitive(
        key = key,
        value = "${typed.resourceAddress}:${typed.nonFungibleId}",
        valueType = MetadataType.NonFungibleGlobalId
    )

    is MetadataNonFungibleGlobalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = "${it.resourceAddress}:${it.nonFungibleId}",
                valueType = MetadataType.NonFungibleGlobalId
            )
        }
    )

    is MetadataNonFungibleLocalIdValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.NonFungibleLocalId
    )

    is MetadataNonFungibleLocalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.NonFungibleLocalId
            )
        }
    )

    is MetadataOriginValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url
    )

    is MetadataOriginArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url
            )
        }
    )

    is MetadataStringValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.String
    )

    is MetadataStringArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.String
            )
        }
    )

    is MetadataUrlValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url
    )

    is MetadataUrlArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url
            )
        }
    )

    is MetadataPublicKeyValue -> when (typed.value) {
        is PublicKeyEcdsaSecp256k1 -> Metadata.Primitive(
            key = key,
            value = typed.value.keyHex,
            valueType = MetadataType.PublicKeyEcdsaSecp256k1
        )

        is PublicKeyEddsaEd25519 -> Metadata.Primitive(
            key = key,
            value = typed.value.keyHex,
            valueType = MetadataType.PublicKeyEddsaEd25519
        )

        else -> error("Not supported MetadataPublicKeyValue type for $value")
    }

    is MetadataPublicKeyArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map { value ->
            when (value) {
                is PublicKeyEcdsaSecp256k1 -> Metadata.Primitive(
                    key = key,
                    value = value.keyHex,
                    valueType = MetadataType.PublicKeyEcdsaSecp256k1
                )

                is PublicKeyEddsaEd25519 -> Metadata.Primitive(
                    key = key,
                    value = value.keyHex,
                    valueType = MetadataType.PublicKeyEddsaEd25519
                )

                else -> error("Not supported MetadataPublicKeyValue type for $value")
            }
        }
    )

    is MetadataPublicKeyHashValue -> when (typed.value) {
        is PublicKeyHashEcdsaSecp256k1 -> Metadata.Primitive(
            key = key,
            value = typed.value.hashHex,
            valueType = MetadataType.PublicKeyHashEcdsaSecp256k1
        )

        is PublicKeyHashEddsaEd25519 -> Metadata.Primitive(
            key = key,
            value = typed.value.hashHex,
            valueType = MetadataType.PublicKeyHashEddsaEd25519
        )
    }

    is MetadataPublicKeyHashArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map { value ->
            when (value) {
                is PublicKeyHashEcdsaSecp256k1 -> Metadata.Primitive(
                    key = key,
                    value = value.hashHex,
                    valueType = MetadataType.PublicKeyHashEcdsaSecp256k1
                )

                is PublicKeyHashEddsaEd25519 -> Metadata.Primitive(
                    key = key,
                    value = value.hashHex,
                    valueType = MetadataType.PublicKeyHashEddsaEd25519
                )
                else -> error("Not supported MetadataPublicKeyHashArrayValue type for $value")
            }
        }
    )

    else -> null
}
