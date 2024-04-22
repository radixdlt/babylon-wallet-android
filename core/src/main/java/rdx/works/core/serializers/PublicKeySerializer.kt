package rdx.works.core.serializers

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PublicKeySerializer : KSerializer<PublicKey> {

    private const val SERIAL_NAME = "rdx.works.core.serializers.PublicKeySerializer"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): PublicKey {
        return PublicKey.init(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: PublicKey) {
        encoder.encodeString(value.hex)
    }
}