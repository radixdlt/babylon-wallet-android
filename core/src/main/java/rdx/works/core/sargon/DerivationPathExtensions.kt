package rdx.works.core.sargon

import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.Cap26Path
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.GetIdPath
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.bip44LikePathGetAddressIndex
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.newAccountPath
import com.radixdlt.sargon.newBip44LikePathFromIndex
import com.radixdlt.sargon.newIdentityPath

fun DerivationPath.Companion.getIdPath() = DerivationPath.Cap26(Cap26Path.GetId(GetIdPath.default()))

fun DerivationPath.Companion.account(
    networkId: NetworkId,
    keyKind: Cap26KeyKind,
    index: HdPathComponent
) = DerivationPath.Cap26(Cap26Path.Account(newAccountPath(networkId = networkId, keyKind = keyKind, index = index.value)))

fun DerivationPath.Companion.identity(
    networkId: NetworkId,
    keyKind: Cap26KeyKind,
    index: HdPathComponent
) = DerivationPath.Cap26(Cap26Path.Identity(newIdentityPath(networkId = networkId, keyKind = keyKind, index = index.value)))

fun DerivationPath.Companion.legacyOlympia(index: HdPathComponent) = DerivationPath.Bip44Like(newBip44LikePathFromIndex(index.value))

val DerivationPath.Bip44Like.index: UInt
    get() = bip44LikePathGetAddressIndex(path = value)

val DerivationPath.entityIndex: UInt? // TODO Integration
    get() = when (this) {
        is DerivationPath.Bip44Like -> index
        is DerivationPath.Cap26 -> when (val path = value) {
            is Cap26Path.GetId -> null
            is Cap26Path.Account -> path.value.index
            is Cap26Path.Identity -> path.value.index
        }
    }

fun Cap26Path.Account.toAuthSigningDerivationPath(): DerivationPath = DerivationPath.account(
    networkId = value.networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = HdPathComponent(value.index)
)

fun Cap26Path.Identity.toAuthSigningDerivationPath(): DerivationPath = DerivationPath.identity(
    networkId = value.networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = HdPathComponent(value.index)
)

fun Bip44LikePath.toAccountAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = DerivationPath.account(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = HdPathComponent(bip44LikePathGetAddressIndex(this))
)

fun Bip44LikePath.toIdentityAuthSigningDerivationPath(networkId: NetworkId): DerivationPath = DerivationPath.identity(
    networkId = networkId,
    keyKind = Cap26KeyKind.AUTHENTICATION_SIGNING,
    index = HdPathComponent(bip44LikePathGetAddressIndex(path = this))
)