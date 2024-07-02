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
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.MetadataType.Integer.Size

fun EntityMetadataCollection.toMetadata(): List<Metadata> {
    return items.mapNotNull { item -> item.toMetadata() }
}

@Suppress("CyclomaticComplexMethod", "LongMethod")
fun EntityMetadataItem.toMetadata(): Metadata? = when (val typed = value.typed) {
    is MetadataBoolValue -> Metadata.Primitive(
        key = key,
        value = typed.value.toString(),
        valueType = MetadataType.Bool,
        isLocked = isLocked
    )

    is MetadataBoolArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it.toString(),
                valueType = MetadataType.Bool,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataDecimalValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Decimal,
        isLocked = isLocked
    )

    is MetadataDecimalArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Decimal
            )
        },
        isLocked = isLocked
    )

    is MetadataGlobalAddressValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Address,
        isLocked = isLocked
    )

    is MetadataGlobalAddressArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Address,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataI32Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = true, size = Size.INT),
        isLocked = isLocked
    )

    is MetadataI32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = true, size = Size.INT),
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataI64Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = true, size = Size.LONG),
        isLocked = isLocked
    )

    is MetadataI64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = true, size = Size.LONG),
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataU8Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.INT),
        isLocked = isLocked
    )

    is MetadataU8ArrayValue -> Metadata.Primitive(
        key = key,
        value = typed.valueHex,
        valueType = MetadataType.Bytes,
        isLocked = isLocked
    )

    is MetadataU32Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.INT),
        isLocked = isLocked
    )

    is MetadataU32ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = false, size = Size.INT),
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataU64Value -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Integer(signed = false, size = Size.LONG),
        isLocked = isLocked
    )

    is MetadataU64ArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Integer(signed = false, size = Size.LONG),
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataInstantValue -> Metadata.Primitive(
        key = key,
        value = typed.unixTimestampSeconds,
        valueType = MetadataType.Instant,
        isLocked = isLocked
    )

    is MetadataInstantArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Instant,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataNonFungibleGlobalIdValue -> Metadata.Primitive(
        key = key,
        value = NonFungibleGlobalId(
            resourceAddress = ResourceAddress.init(typed.resourceAddress),
            nonFungibleLocalId = NonFungibleLocalId.init(typed.nonFungibleId)
        ).string,
        valueType = MetadataType.NonFungibleGlobalId,
        isLocked = isLocked
    )

    is MetadataNonFungibleGlobalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = NonFungibleGlobalId(
                    resourceAddress = ResourceAddress.init(it.resourceAddress),
                    nonFungibleLocalId = NonFungibleLocalId.init(it.nonFungibleId)
                ).string,
                valueType = MetadataType.NonFungibleGlobalId,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataNonFungibleLocalIdValue -> Metadata.Primitive(
        key = key,
        lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
        value = typed.value,
        valueType = MetadataType.NonFungibleLocalId,
        isLocked = isLocked
    )

    is MetadataNonFungibleLocalIdArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.NonFungibleLocalId,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataOriginValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url,
        isLocked = isLocked
    )

    is MetadataOriginArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataStringValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.String,
        isLocked = isLocked
    )

    is MetadataStringArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.String,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataUrlValue -> Metadata.Primitive(
        key = key,
        value = typed.value,
        valueType = MetadataType.Url,
        isLocked = isLocked
    )

    is MetadataUrlArrayValue -> Metadata.Collection(
        key = key,
        values = typed.propertyValues.map {
            Metadata.Primitive(
                key = key,
                value = it,
                valueType = MetadataType.Url,
                isLocked = isLocked
            )
        },
        isLocked = isLocked
    )

    is MetadataPublicKeyValue -> when (typed.value) {
        is PublicKeyEcdsaSecp256k1 -> Metadata.Primitive(
            key = key,
            lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
            value = typed.value.keyHex,
            valueType = MetadataType.PublicKeyEcdsaSecp256k1,
            isLocked = isLocked
        )

        is PublicKeyEddsaEd25519 -> Metadata.Primitive(
            key = key,
            lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
            value = typed.value.keyHex,
            valueType = MetadataType.PublicKeyEddsaEd25519,
            isLocked = isLocked
        )

        else -> error("Not supported MetadataPublicKeyValue type for $value")
    }

    is MetadataPublicKeyArrayValue -> Metadata.Collection(
        key = key,
        lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
        values = typed.propertyValues.map { value ->
            when (value) {
                is PublicKeyEcdsaSecp256k1 -> Metadata.Primitive(
                    key = key,
                    value = value.keyHex,
                    valueType = MetadataType.PublicKeyEcdsaSecp256k1,
                    isLocked = isLocked
                )

                is PublicKeyEddsaEd25519 -> Metadata.Primitive(
                    key = key,
                    value = value.keyHex,
                    valueType = MetadataType.PublicKeyEddsaEd25519,
                    isLocked = isLocked
                )

                else -> error("Not supported MetadataPublicKeyValue type for $value")
            }
        },
        isLocked = isLocked
    )

    is MetadataPublicKeyHashValue -> when (typed.value) {
        is PublicKeyHashEcdsaSecp256k1 -> Metadata.Primitive(
            key = key,
            lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
            value = typed.value.hashHex,
            valueType = MetadataType.PublicKeyHashEcdsaSecp256k1,
            isLocked = isLocked
        )

        is PublicKeyHashEddsaEd25519 -> Metadata.Primitive(
            key = key,
            lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
            value = typed.value.hashHex,
            valueType = MetadataType.PublicKeyHashEddsaEd25519,
            isLocked = isLocked
        )
    }

    is MetadataPublicKeyHashArrayValue -> Metadata.Collection(
        key = key,
        lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
        values = typed.propertyValues.map { value ->
            when (value) {
                is PublicKeyHashEcdsaSecp256k1 -> Metadata.Primitive(
                    key = key,
                    lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
                    value = value.hashHex,
                    valueType = MetadataType.PublicKeyHashEcdsaSecp256k1,
                    isLocked = isLocked
                )

                is PublicKeyHashEddsaEd25519 -> Metadata.Primitive(
                    key = key,
                    lastUpdatedAtStateVersion = lastUpdatedAtStateVersion,
                    value = value.hashHex,
                    valueType = MetadataType.PublicKeyHashEddsaEd25519,
                    isLocked = isLocked
                )
            }
        },
        isLocked = isLocked
    )

    else -> null
}
