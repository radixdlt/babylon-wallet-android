package com.babylon.wallet.android.data.gateway.generated.infrastructure

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.concurrent.atomic.AtomicLong

@Serializer(forClass = AtomicLong::class)
object AtomicLongAdapter : KSerializer<AtomicLong> {
    override fun serialize(encoder: Encoder, value: AtomicLong) {
        encoder.encodeLong(value.get())
    }

    override fun deserialize(decoder: Decoder): AtomicLong = AtomicLong(decoder.decodeLong())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AtomicLong", PrimitiveKind.LONG)
}
