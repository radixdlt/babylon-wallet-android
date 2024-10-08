package rdx.works.core.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.temporal.ChronoUnit

object InstantSerializer : KSerializer<Instant> {

    private const val SERIAL_NAME = "java.time.Instant"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Instant) {
        val truncated = value.truncatedTo(ChronoUnit.SECONDS)
        encoder.encodeString(truncated.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString()).truncatedTo(ChronoUnit.SECONDS)
    }
}
