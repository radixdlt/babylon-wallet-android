package rdx.works.core.sargon

import com.radixdlt.sargon.AccountPath
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.IdentityPath
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asHardened
import com.radixdlt.sargon.extensions.init

fun AccountPath.toAuthSigningDerivationPath(): DerivationPath = AccountPath.init(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = index
).asGeneral()

fun IdentityPath.toAuthSigningDerivationPath(): DerivationPath = IdentityPath.init(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = index
).asGeneral()

fun Bip44LikePath.toAccountAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = AccountPath.init(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = index.asHardened()
).asGeneral()

fun Bip44LikePath.toIdentityAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = IdentityPath.init(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = index.asHardened()
).asGeneral()
