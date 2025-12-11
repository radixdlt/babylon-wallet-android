package com.babylon.wallet.android.data.gateway.serialisers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

object AnyAsJsonElementSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("AnyAsJsonElementSerializer supports only JSON")
        return jsonDecoder.decodeJsonElement()
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("AnyAsJsonElementSerializer supports only JSON")
        val element = value as? JsonElement
            ?: error("Expected JsonElement when serializing Any")
        jsonEncoder.encodeJsonElement(element)
    }
}
