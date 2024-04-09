package rdx.works.core.domain

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun Decimal192.roundedWith(divisibility: UByte?) = if (divisibility != null) {
    rounded(divisibility) // TODO decimal192
} else {
    this
}

fun Decimal192.toDouble() = this.string.toDouble() // TODO decimal192

val Decimal192.Companion.Serializer: KSerializer<Decimal192>
    get() = object : KSerializer<Decimal192> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Decimal192", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Decimal192 = decoder.decodeString().toDecimal192()

        override fun serialize(encoder: Encoder, value: Decimal192) = encoder.encodeString(value.string)
    }
