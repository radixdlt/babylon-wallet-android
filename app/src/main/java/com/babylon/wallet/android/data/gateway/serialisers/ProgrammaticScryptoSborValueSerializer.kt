package com.babylon.wallet.android.data.gateway.serialisers

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
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueKind
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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProgrammaticScryptoSborValueSerializer: JsonContentPolymorphicSerializer<ProgrammaticScryptoSborValue>(ProgrammaticScryptoSborValue::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ProgrammaticScryptoSborValue> {
        return when (ProgrammaticScryptoSborValueKind.decode(element.jsonObject["kind"]?.jsonPrimitive?.content.orEmpty())) {
            ProgrammaticScryptoSborValueKind.bool -> ProgrammaticScryptoSborValueBool.serializer()
            ProgrammaticScryptoSborValueKind.i8 -> ProgrammaticScryptoSborValueI8.serializer()
            ProgrammaticScryptoSborValueKind.i16 -> ProgrammaticScryptoSborValueI16.serializer()
            ProgrammaticScryptoSborValueKind.i32 -> ProgrammaticScryptoSborValueI32.serializer()
            ProgrammaticScryptoSborValueKind.i64 -> ProgrammaticScryptoSborValueI64.serializer()
            ProgrammaticScryptoSborValueKind.i128 -> ProgrammaticScryptoSborValueI128.serializer()
            ProgrammaticScryptoSborValueKind.u8 -> ProgrammaticScryptoSborValueU8.serializer()
            ProgrammaticScryptoSborValueKind.u16 -> ProgrammaticScryptoSborValueU16.serializer()
            ProgrammaticScryptoSborValueKind.u32 -> ProgrammaticScryptoSborValueU32.serializer()
            ProgrammaticScryptoSborValueKind.u64 -> ProgrammaticScryptoSborValueU64.serializer()
            ProgrammaticScryptoSborValueKind.u128 -> ProgrammaticScryptoSborValueU128.serializer()
            ProgrammaticScryptoSborValueKind.string -> ProgrammaticScryptoSborValueString.serializer()
            ProgrammaticScryptoSborValueKind.enum -> ProgrammaticScryptoSborValueEnum.serializer()
            ProgrammaticScryptoSborValueKind.array -> ProgrammaticScryptoSborValueArray.serializer()
            ProgrammaticScryptoSborValueKind.bytes -> ProgrammaticScryptoSborValueBytes.serializer()
            ProgrammaticScryptoSborValueKind.map -> ProgrammaticScryptoSborValueMap.serializer()
            ProgrammaticScryptoSborValueKind.tuple -> ProgrammaticScryptoSborValueTuple.serializer()
            ProgrammaticScryptoSborValueKind.reference -> ProgrammaticScryptoSborValueReference.serializer()
            ProgrammaticScryptoSborValueKind.own -> ProgrammaticScryptoSborValueOwn.serializer()
            ProgrammaticScryptoSborValueKind.decimal -> ProgrammaticScryptoSborValueDecimal.serializer()
            ProgrammaticScryptoSborValueKind.preciseDecimal -> ProgrammaticScryptoSborValuePreciseDecimal.serializer()
            ProgrammaticScryptoSborValueKind.nonFungibleLocalId -> ProgrammaticScryptoSborValueNonFungibleLocalId.serializer()
            else -> error("No such ProgrammaticScryptoSborValueKind of ${element.jsonObject["kind"]}")
        }
    }
}
