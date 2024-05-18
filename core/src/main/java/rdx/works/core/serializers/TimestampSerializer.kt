package rdx.works.core.serializers

import com.radixdlt.sargon.Timestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter

object TimestampSerializer : KSerializer<Timestamp> {

    private const val SERIAL_NAME = "com.radixdlt.sargon.Timestamp"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE_TIME))
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        return Timestamp.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
    }
}
