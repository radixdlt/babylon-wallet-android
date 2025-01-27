package rdx.works.core.sargon

import com.radixdlt.sargon.DerivePublicKeysSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init

fun DerivePublicKeysSource.toFactorSourceId(): FactorSourceId.Hash = when (this) {
    is DerivePublicKeysSource.FactorSource -> v1.asGeneral()
    is DerivePublicKeysSource.Mnemonic -> FactorSourceId.Hash.init(
        kind = FactorSourceKind.DEVICE,
        mnemonicWithPassphrase = v1
    )
}