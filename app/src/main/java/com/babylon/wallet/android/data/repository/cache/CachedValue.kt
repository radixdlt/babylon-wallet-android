package com.babylon.wallet.android.data.repository.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable
data class CachedValue<T>(
    val cached: T,
    val timestamp: Long
)

class CachedValueSerializer<T>(
    private val valueSerializer: KSerializer<T>
) : KSerializer<CachedValue<T>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CachedValue") {
        element("cached", valueSerializer.descriptor)
        element<Long>("timestamp")
    }

    override fun serialize(encoder: Encoder, value: CachedValue<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, valueSerializer, value.cached)
            encodeLongElement(descriptor, 1, value.timestamp)
        }
    }

    override fun deserialize(decoder: Decoder): CachedValue<T> = decoder.decodeStructure(descriptor) {
        var value: T? = null
        var timestamp: Long? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> value = decodeSerializableElement(descriptor, 0, valueSerializer)
                1 -> timestamp = decodeLongElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unknown index: $index")
            }
        }
        requireNotNull(value)
        requireNotNull(timestamp)

        CachedValue(value, timestamp)
    }

}
