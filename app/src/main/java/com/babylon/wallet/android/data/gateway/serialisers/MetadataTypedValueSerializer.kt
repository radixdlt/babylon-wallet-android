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

object MetadataTypedValueSerializer : JsonContentPolymorphicSerializer<MetadataTypedValue>(MetadataTypedValue::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MetadataTypedValue> {
        return when (MetadataValueType.from(element.jsonObject["type"]?.jsonPrimitive?.content.orEmpty())) {
            MetadataValueType.string -> MetadataStringValue.serializer()
            MetadataValueType.bool -> MetadataBoolValue.serializer()
            MetadataValueType.u8 -> MetadataU8Value.serializer()
            MetadataValueType.u32 -> MetadataU32Value.serializer()
            MetadataValueType.u64 -> MetadataU64Value.serializer()
            MetadataValueType.i32 -> MetadataI32Value.serializer()
            MetadataValueType.i64 -> MetadataI64Value.serializer()
            MetadataValueType.decimal -> MetadataDecimalValue.serializer()
            MetadataValueType.globalAddress -> MetadataGlobalAddressValue.serializer()
            MetadataValueType.publicKey -> MetadataPublicKeyValue.serializer()
            MetadataValueType.nonFungibleGlobalId -> MetadataNonFungibleGlobalIdValue.serializer()
            MetadataValueType.nonFungibleLocalId -> MetadataNonFungibleLocalIdValue.serializer()
            MetadataValueType.instant -> MetadataInstantValue.serializer()
            MetadataValueType.url -> MetadataUrlValue.serializer()
            MetadataValueType.origin -> MetadataOriginValue.serializer()
            MetadataValueType.publicKeyHash -> MetadataPublicKeyHashValue.serializer()
            MetadataValueType.stringArray -> MetadataStringArrayValue.serializer()
            MetadataValueType.boolArray -> MetadataBoolArrayValue.serializer()
            MetadataValueType.u8Array -> MetadataU8ArrayValue.serializer()
            MetadataValueType.u32Array -> MetadataU32Value.serializer()
            MetadataValueType.u64Array -> MetadataU64ArrayValue.serializer()
            MetadataValueType.i32Array -> MetadataI32ArrayValue.serializer()
            MetadataValueType.i64Array -> MetadataI64ArrayValue.serializer()
            MetadataValueType.decimalArray -> MetadataDecimalArrayValue.serializer()
            MetadataValueType.globalAddressArray -> MetadataGlobalAddressArrayValue.serializer()
            MetadataValueType.publicKeyArray -> MetadataPublicKeyArrayValue.serializer()
            MetadataValueType.nonFungibleGlobalIdArray -> MetadataNonFungibleGlobalIdArrayValue.serializer()
            MetadataValueType.nonFungibleLocalIdArray -> MetadataNonFungibleLocalIdArrayValue.serializer()
            MetadataValueType.instantArray -> MetadataInstantArrayValue.serializer()
            MetadataValueType.urlArray -> MetadataUrlArrayValue.serializer()
            MetadataValueType.originArray -> MetadataOriginArrayValue.serializer()
            MetadataValueType.publicKeyHashArray -> MetadataPublicKeyHashArrayValue.serializer()
        }
    }
}
