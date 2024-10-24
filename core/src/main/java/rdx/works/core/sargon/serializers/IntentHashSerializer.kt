package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TransactionIntentHashSerializer : KSerializer<TransactionIntentHash> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.TransactionIntentHash", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TransactionIntentHash) {
        encoder.encodeString(value.bech32EncodedTxId)
    }

    override fun deserialize(decoder: Decoder): TransactionIntentHash {
        return TransactionIntentHash.init(decoder.decodeString())
    }
}
