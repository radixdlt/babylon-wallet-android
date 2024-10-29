package rdx.works.core.sargon

import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.Hardened
import com.radixdlt.sargon.extensions.asHardened
import com.radixdlt.sargon.extensions.indexInLocalKeySpace
import com.radixdlt.sargon.extensions.init

fun AppearanceId.Companion.from(derivationPath: DerivationPath): AppearanceId {
    val hardened = when (derivationPath) {
        is DerivationPath.Account -> derivationPath.value.index
        is DerivationPath.Bip44Like -> derivationPath.value.index.asHardened()
        is DerivationPath.Identity -> derivationPath.value.index
    }

    val index = when (hardened) {
        is Hardened.Securified -> hardened.v1.indexInLocalKeySpace
        is Hardened.Unsecurified -> hardened.v1.indexInLocalKeySpace
    }

    return from(offset = index)
}

fun AppearanceId.Companion.from(offset: UInt): AppearanceId = AppearanceId.init((offset % AppearanceId.MAX).toUByte())

val AppearanceId.Companion.MAX: UByte
    get() = 12u
