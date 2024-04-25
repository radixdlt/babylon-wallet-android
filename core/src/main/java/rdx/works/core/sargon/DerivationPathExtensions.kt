package rdx.works.core.sargon

import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.GetIdPath
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.addressIndex
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.extensions.identity

fun DerivationPath.Companion.getIdPath() = DerivationPath.Cap26(Cap26Path.GetId(GetIdPath.default()))

fun Cap26Path.Account.toAuthSigningDerivationPath(): DerivationPath = DerivationPath.Cap26.account(
    networkId = value.networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = value.index
)

fun Cap26Path.Identity.toAuthSigningDerivationPath(): DerivationPath = DerivationPath.Cap26.identity(
    networkId = value.networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = value.index
)

fun Bip44LikePath.toAccountAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = DerivationPath.Cap26.account(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = asGeneral().addressIndex
)

fun Bip44LikePath.toIdentityAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = DerivationPath.Cap26.identity(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = asGeneral().addressIndex
)
