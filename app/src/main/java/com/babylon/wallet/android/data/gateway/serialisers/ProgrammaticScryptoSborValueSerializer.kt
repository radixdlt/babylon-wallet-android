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

class ProgrammaticScryptoSborValueSerializer : JsonContentPolymorphicSerializer<ProgrammaticScryptoSborValue>(
    ProgrammaticScryptoSborValue::class
) {
    @Suppress("CyclomaticComplexMethod")
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ProgrammaticScryptoSborValue> {
        return when (ProgrammaticScryptoSborValueKind.decode(element.jsonObject["kind"]?.jsonPrimitive?.content.orEmpty())) {
            ProgrammaticScryptoSborValueKind.Bool -> ProgrammaticScryptoSborValueBool.serializer()
            ProgrammaticScryptoSborValueKind.I8 -> ProgrammaticScryptoSborValueI8.serializer()
            ProgrammaticScryptoSborValueKind.I16 -> ProgrammaticScryptoSborValueI16.serializer()
            ProgrammaticScryptoSborValueKind.I32 -> ProgrammaticScryptoSborValueI32.serializer()
            ProgrammaticScryptoSborValueKind.I64 -> ProgrammaticScryptoSborValueI64.serializer()
            ProgrammaticScryptoSborValueKind.I128 -> ProgrammaticScryptoSborValueI128.serializer()
            ProgrammaticScryptoSborValueKind.U8 -> ProgrammaticScryptoSborValueU8.serializer()
            ProgrammaticScryptoSborValueKind.U16 -> ProgrammaticScryptoSborValueU16.serializer()
            ProgrammaticScryptoSborValueKind.U32 -> ProgrammaticScryptoSborValueU32.serializer()
            ProgrammaticScryptoSborValueKind.U64 -> ProgrammaticScryptoSborValueU64.serializer()
            ProgrammaticScryptoSborValueKind.U128 -> ProgrammaticScryptoSborValueU128.serializer()
            ProgrammaticScryptoSborValueKind.String -> ProgrammaticScryptoSborValueString.serializer()
            ProgrammaticScryptoSborValueKind.Enum -> ProgrammaticScryptoSborValueEnum.serializer()
            ProgrammaticScryptoSborValueKind.Array -> ProgrammaticScryptoSborValueArray.serializer()
            ProgrammaticScryptoSborValueKind.Bytes -> ProgrammaticScryptoSborValueBytes.serializer()
            ProgrammaticScryptoSborValueKind.Map -> ProgrammaticScryptoSborValueMap.serializer()
            ProgrammaticScryptoSborValueKind.Tuple -> ProgrammaticScryptoSborValueTuple.serializer()
            ProgrammaticScryptoSborValueKind.Reference -> ProgrammaticScryptoSborValueReference.serializer()
            ProgrammaticScryptoSborValueKind.Own -> ProgrammaticScryptoSborValueOwn.serializer()
            ProgrammaticScryptoSborValueKind.Decimal -> ProgrammaticScryptoSborValueDecimal.serializer()
            ProgrammaticScryptoSborValueKind.PreciseDecimal -> ProgrammaticScryptoSborValuePreciseDecimal.serializer()
            ProgrammaticScryptoSborValueKind.NonFungibleLocalId -> ProgrammaticScryptoSborValueNonFungibleLocalId.serializer()
            else -> error("No such ProgrammaticScryptoSborValueKind of ${element.jsonObject["kind"]}")
        }
    }
}
