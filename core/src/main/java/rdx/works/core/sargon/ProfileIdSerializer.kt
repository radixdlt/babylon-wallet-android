package rdx.works.core.sargon

import com.radixdlt.sargon.ProfileId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ProfileIdSerializer : KSerializer<ProfileId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ProfileId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ProfileId) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ProfileId {
        return ProfileId.fromString(decoder.decodeString())
    }
}
