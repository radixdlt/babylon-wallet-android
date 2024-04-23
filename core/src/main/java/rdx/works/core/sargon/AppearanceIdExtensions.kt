package rdx.works.core.sargon

import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.extensions.init

fun AppearanceId.Companion.from(derivationPath: DerivationPath): AppearanceId {
    val entityIndex = derivationPath.entityIndex ?: 0u

    return from(offset = entityIndex)
}

fun AppearanceId.Companion.from(offset: UInt): AppearanceId = AppearanceId.init((offset % AppearanceId.MAX).toUByte())

val AppearanceId.Companion.MAX: UByte
    get() = 12u