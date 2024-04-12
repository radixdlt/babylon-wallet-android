package rdx.works.core.sargon

import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath

val DerivationPath.entityIndex: UInt?
    get() = when (this) {
        is DerivationPath.Bip44Like -> null
        is DerivationPath.Cap26 -> when (val path = value) {
            is Cap26Path.GetId -> null
            is Cap26Path.Account -> path.value.index
            is Cap26Path.Identity -> path.value.index
        }
    }