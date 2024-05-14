package rdx.works.core.serializers

import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SignatureSerializer : KSerializer<Signature> {

    private const val SERIAL_NAME = "rdx.works.core.serializers.SignatureSerializer"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): Signature {
        return Signature.init(decoder.decodeString().hexToBagOfBytes())
    }

    override fun serialize(encoder: Encoder, value: Signature) {
        encoder.encodeString(value.string)
    }
}
