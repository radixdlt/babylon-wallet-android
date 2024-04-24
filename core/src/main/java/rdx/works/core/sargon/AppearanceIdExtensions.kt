package rdx.works.core.sargon

import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.nonHardenedIndex

fun AppearanceId.Companion.from(derivationPath: DerivationPath): AppearanceId {
    return from(offset = derivationPath.nonHardenedIndex)
}

fun AppearanceId.Companion.from(offset: UInt): AppearanceId = AppearanceId.init((offset % AppearanceId.MAX).toUByte())

val AppearanceId.Companion.MAX: UByte
    get() = 12u