package rdx.works.profile.sargon

import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.toDecimal192
import java.math.BigDecimal

// Temporary until all values are replaced with Decimal192
@Suppress("SwallowedException")
fun BigDecimal.toDecimal192(): Decimal192 = try {
    toPlainString().toDecimal192()
} catch (exception: CommonException.DecimalException) {
    toFloat().toDecimal192()
}
