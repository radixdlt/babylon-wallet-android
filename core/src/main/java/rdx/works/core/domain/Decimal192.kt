package rdx.works.core.domain

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import rdx.works.core.domain.resources.Divisibility

fun Decimal192.roundedWith(divisibility: Divisibility?) = if (divisibility != null) {
    rounded(divisibility.value)
} else {
    this
}

// This will later be migrated to Sargon
fun Decimal192.toDouble() = formatted(useGroupingSeparator = false).toDouble()

val Decimal192.Companion.Serializer: KSerializer<Decimal192>
    get() = object : KSerializer<Decimal192> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Decimal192", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Decimal192 = decoder.decodeString().toDecimal192()

        override fun serialize(encoder: Encoder, value: Decimal192) = encoder.encodeString(value.string)
    }
