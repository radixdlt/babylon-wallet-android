package rdx.works.core.sargon

import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.extensions.indexInGlobalKeySpace
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.lastPathComponent

fun AppearanceId.Companion.from(derivationPath: DerivationPath): AppearanceId {
    val index = when (derivationPath) {
        is DerivationPath.Account, is DerivationPath.Identity -> derivationPath.lastPathComponent.indexInGlobalKeySpace
        is DerivationPath.Bip44Like -> derivationPath.value.index.indexInGlobalKeySpace
    }

    return from(offset = index)
}

fun AppearanceId.Companion.from(offset: UInt): AppearanceId = AppearanceId.init((offset % AppearanceId.MAX).toUByte())

val AppearanceId.Companion.MAX: UByte
    get() = 12u
