package rdx.works.profile.sargon

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.toDecimal192
import java.math.BigDecimal

// Temporary until all values are replaced with Decimal192
fun BigDecimal.toDecimal192(): Decimal192 = toPlainString().toDecimal192()