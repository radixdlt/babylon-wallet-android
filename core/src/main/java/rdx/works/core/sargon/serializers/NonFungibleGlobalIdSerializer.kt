package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class NonFungibleGlobalIdSerializer: KSerializer<NonFungibleGlobalId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.radixdlt.sargon.NonFungibleGlobalId",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: NonFungibleGlobalId) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): NonFungibleGlobalId {
        return NonFungibleGlobalId.init(decoder.decodeString())
    }
}