package rdx.works.core.serializers

import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StringBase64EncodedSerializer : KSerializer<String> {
    private const val SERIAL_NAME = "rdx.works.core.serializers.StringBase64EncodedSerializer"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value.encodeBase64())
    }

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString().decodeBase64String()
    }
}
