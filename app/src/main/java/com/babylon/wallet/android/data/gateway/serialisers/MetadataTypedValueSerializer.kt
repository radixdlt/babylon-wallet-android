package com.babylon.wallet.android.data.gateway.serialisers

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
import com.babylon.wallet.android.data.gateway.generated.models.MetadataTypedValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU32ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU32Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU64ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU64Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU8ArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataU8Value
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlArrayValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataUrlValue
import com.babylon.wallet.android.data.gateway.generated.models.MetadataValueType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Suppress("CyclomaticComplexMethod")
object MetadataTypedValueSerializer : JsonContentPolymorphicSerializer<MetadataTypedValue>(MetadataTypedValue::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MetadataTypedValue> {
        return when (MetadataValueType.decode(element.jsonObject["type"]?.jsonPrimitive?.content.orEmpty())) {
            MetadataValueType.String -> MetadataStringValue.serializer()
            MetadataValueType.Bool -> MetadataBoolValue.serializer()
            MetadataValueType.U8 -> MetadataU8Value.serializer()
            MetadataValueType.U32 -> MetadataU32Value.serializer()
            MetadataValueType.U64 -> MetadataU64Value.serializer()
            MetadataValueType.I32 -> MetadataI32Value.serializer()
            MetadataValueType.I64 -> MetadataI64Value.serializer()
            MetadataValueType.Decimal -> MetadataDecimalValue.serializer()
            MetadataValueType.GlobalAddress -> MetadataGlobalAddressValue.serializer()
            MetadataValueType.PublicKey -> MetadataPublicKeyValue.serializer()
            MetadataValueType.NonFungibleGlobalId -> MetadataNonFungibleGlobalIdValue.serializer()
            MetadataValueType.NonFungibleLocalId -> MetadataNonFungibleLocalIdValue.serializer()
            MetadataValueType.Instant -> MetadataInstantValue.serializer()
            MetadataValueType.Url -> MetadataUrlValue.serializer()
            MetadataValueType.Origin -> MetadataOriginValue.serializer()
            MetadataValueType.PublicKeyHash -> MetadataPublicKeyHashValue.serializer()
            MetadataValueType.StringArray -> MetadataStringArrayValue.serializer()
            MetadataValueType.BoolArray -> MetadataBoolArrayValue.serializer()
            MetadataValueType.U8Array -> MetadataU8ArrayValue.serializer()
            MetadataValueType.U32Array -> MetadataU32ArrayValue.serializer()
            MetadataValueType.U64Array -> MetadataU64ArrayValue.serializer()
            MetadataValueType.I32Array -> MetadataI32ArrayValue.serializer()
            MetadataValueType.I64Array -> MetadataI64ArrayValue.serializer()
            MetadataValueType.DecimalArray -> MetadataDecimalArrayValue.serializer()
            MetadataValueType.GlobalAddressArray -> MetadataGlobalAddressArrayValue.serializer()
            MetadataValueType.PublicKeyArray -> MetadataPublicKeyArrayValue.serializer()
            MetadataValueType.NonFungibleGlobalIdArray -> MetadataNonFungibleGlobalIdArrayValue.serializer()
            MetadataValueType.NonFungibleLocalIdArray -> MetadataNonFungibleLocalIdArrayValue.serializer()
            MetadataValueType.InstantArray -> MetadataInstantArrayValue.serializer()
            MetadataValueType.UrlArray -> MetadataUrlArrayValue.serializer()
            MetadataValueType.OriginArray -> MetadataOriginArrayValue.serializer()
            MetadataValueType.PublicKeyHashArray -> MetadataPublicKeyHashArrayValue.serializer()
            else -> error("")
        }
    }
}
