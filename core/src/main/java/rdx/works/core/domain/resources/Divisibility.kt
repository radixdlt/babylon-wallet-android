package rdx.works.core.domain.resources

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.MAX_DIVISIBILITY

data class Divisibility(val value: UByte) {
    init {
        require(value <= Decimal192.MAX_DIVISIBILITY) {
            "Divisibility MUST be 0...18, was $value"
        }
    }
}
