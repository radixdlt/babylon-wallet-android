package rdx.works.core.serializers

import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RadixConnectPasswordSerializer : KSerializer<RadixConnectPassword> {

    private const val SERIAL_NAME = "rdx.works.core.serializers.PublicKeySerializer"

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = SERIAL_NAME,
        kind = PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): RadixConnectPassword {
        return RadixConnectPassword(Exactly32Bytes.init(decoder.decodeString().hexToBagOfBytes()))
    }

    override fun serialize(encoder: Encoder, value: RadixConnectPassword) {
        encoder.encodeString(value.value.hex)
    }
}
