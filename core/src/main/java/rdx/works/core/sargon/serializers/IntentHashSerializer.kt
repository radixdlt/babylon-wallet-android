package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class IntentHashSerializer : KSerializer<IntentHash> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.IntentHash", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntentHash) {
        encoder.encodeString(value.bech32EncodedTxId)
    }

    override fun deserialize(decoder: Decoder): IntentHash {
        return IntentHash.init(decoder.decodeString())
    }
}
