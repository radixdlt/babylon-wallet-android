package rdx.works.core.serializers

import com.radixdlt.sargon.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UuidSerializer: KSerializer<Uuid> {

    private const val SERIAL_NAME = "com.radixdlt.sargon.Uuid"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.fromString(decoder.decodeString())
    }
}