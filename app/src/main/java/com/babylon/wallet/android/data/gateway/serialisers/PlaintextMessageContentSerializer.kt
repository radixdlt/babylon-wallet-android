package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.coreapi.BinaryPlaintextMessageContent
import com.babylon.wallet.android.data.gateway.coreapi.PlaintextMessageContent
import com.babylon.wallet.android.data.gateway.coreapi.PlaintextMessageContentType
import com.babylon.wallet.android.data.gateway.coreapi.StringPlaintextMessageContent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PlaintextMessageContentSerializer : JsonContentPolymorphicSerializer<PlaintextMessageContent>(PlaintextMessageContent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PlaintextMessageContent> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            PlaintextMessageContentType.string.value -> StringPlaintextMessageContent.serializer()
            PlaintextMessageContentType.binary.value -> BinaryPlaintextMessageContent.serializer()
            else -> error("No serializer found for PlaintextMessageContent type.")
        }
    }
}
