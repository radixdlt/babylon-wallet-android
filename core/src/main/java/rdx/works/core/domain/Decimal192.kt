package rdx.works.core.domain

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.LocaleConfig
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.formattedPlain
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.rounded
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.DecimalFormatSymbols

inline fun <T> Iterable<T>.sumOf(selector: (T) -> Decimal192): Decimal192 {
    var sum: Decimal192 = 0.toDecimal192()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun String.toDecimal192OrNull() = runCatching { toDecimal192() }.getOrNull()

fun Decimal192.roundedWith(divisibility: UByte?) = if (divisibility != null) {
    rounded(divisibility) // TODO decimal192
} else {
    this
}

fun Decimal192?.orZero() = this ?: 0.toDecimal192()

fun Decimal192.toDouble() = this.string.toDouble() // TODO decimal192

val Decimal192.Companion.Serializer: KSerializer<Decimal192>
    get() = object : KSerializer<Decimal192> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Decimal192", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Decimal192 = decoder.decodeString().toDecimal192()

        override fun serialize(encoder: Encoder, value: Decimal192) = encoder.encodeString(value.string)
    }

fun Decimal192.formatted(
    totalPlaces: UByte = 8u,
    useGroupingSeparator: Boolean = true
): String {
    val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
    return formatted(
        locale = LocaleConfig(
            decimalSeparator = decimalFormatSymbols.decimalSeparator.toString(),
            groupingSeparator = decimalFormatSymbols.groupingSeparator.toString()
        ),
        totalPlaces = totalPlaces,
        useGroupingSeparator = useGroupingSeparator
    )
}

fun Decimal192.formattedPlain(useGroupingSeparator: Boolean = false): String {
    val decimalFormatSymbols = DecimalFormatSymbols.getInstance()
    return formattedPlain(
        locale = LocaleConfig(
            decimalSeparator = decimalFormatSymbols.decimalSeparator.toString(),
            groupingSeparator = decimalFormatSymbols.groupingSeparator.toString()
        ),
        useGroupingSeparator = useGroupingSeparator
    )
}
