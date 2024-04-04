package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RcrResponseSerializer::class)
class RcrResponse : ArrayList<String>()

@OptIn(ExperimentalSerializationApi::class)
object RcrResponseSerializer : KSerializer<RcrResponse> {
    private val delegateSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor
        get() = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): RcrResponse {
        return try {
            val items = decoder.decodeNullableSerializableValue(delegateSerializer) ?: emptyList()
            RcrResponse().apply {
                addAll(items)
            }
        } catch (e: Exception) {
            RcrResponse()
        }
    }

    override fun serialize(encoder: Encoder, value: RcrResponse) {
        encoder.encodeSerializableValue(delegateSerializer, value)
    }
}