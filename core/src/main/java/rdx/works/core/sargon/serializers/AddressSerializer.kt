package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.Address
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class AddressSerializer: KSerializer<Address> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.Address", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Address) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): Address {
        return Address.init(decoder.decodeString())
    }
}