package rdx.works.profile.data.model.pernetwork

import com.radixdlt.bip44.BIP44_PREFIX
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.CAP_26
import rdx.works.profile.derivation.model.CoinType
import rdx.works.profile.derivation.model.EntityType
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

@Serializable
data class DerivationPath(
    @SerialName("path")
    val path: String,

    @SerialName("scheme")
    val scheme: DerivationPathScheme
) {

    companion object {
        fun forAccount(
            networkId: NetworkId,
            accountIndex: Int,
            keyType: KeyType
        ): DerivationPath = AccountDerivationPathBuilder(
            networkId = networkId,
            accountIndex = accountIndex,
            keyType = keyType
        ).build()

        fun forIdentity(
            networkId: NetworkId,
            identityIndex: Int,
            keyType: KeyType
        ): DerivationPath = IdentityDerivationPathBuilder(
            networkId = networkId,
            identityIndex = identityIndex,
            keyType = keyType
        ).build()

        fun forFactorSource(): DerivationPath = FactorSourceIdDerivationPathBuilder.build()

        fun forLegacyOlympia(accountIndex: Int): DerivationPath = LegacyOlympiaBIP44LikeDerivationPathBuilder(
            accountIndex = accountIndex
        ).build()

        fun authSigningDerivationPathFromCap26Path(derivationPath: DerivationPath): DerivationPath {
            val pathComponents = derivationPath.path.split("/").drop(1).toMutableList()
            require(pathComponents.size == BabylonDerivationPathComponent.values().size)
            pathComponents.removeAt(BabylonDerivationPathComponent.KeyKindIndex.ordinal)
            pathComponents.add(BabylonDerivationPathComponent.KeyKindIndex.ordinal, "${KeyType.AUTHENTICATION_SIGNING.value}H")
            return DerivationPath(
                path = "m/" + pathComponents.joinToString("/"),
                scheme = CAP_26
            )
        }

        fun authSigningDerivationPathFromBip44LikePath(networkId: NetworkId, derivationPath: DerivationPath): DerivationPath {
            val pathComponents = derivationPath.path.split("/").drop(1).toMutableList()
            require(pathComponents.size == OlympiaDerivationPathComponent.values().size)
            val accountIndex = pathComponents.last().lowercase().replace("h", "").toInt()
            return forAccount(networkId, accountIndex, KeyType.AUTHENTICATION_SIGNING)
        }
    }
}

/**
 * Defines the derivation path used with SLIP10 algorithm to derive private keys.
 *
 * For more info for [CAP-26](https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2897772650/CAP-26+SLIP10+HD+Derivation+Path+Scheme)
 */
private sealed interface DerivationPathBuilder {
    fun build(): DerivationPath
}

/**
 * The **default** derivation path used to derive `Account` keys for signing off transactions or for
 * signing authentication, at a certain account index (`ENTITY_INDEX`) and **unique per network** (`NETWORK_ID`)
 * as per CAP-26.
 *
 * The format is:
 *      `m/44'/1022'/<NETWORK_ID>'/525'/<KEY_TYPE>'/<ENTITY_INDEX>'`
 */
private data class AccountDerivationPathBuilder(
    private val networkId: NetworkId,
    private val accountIndex: Int,
    private val keyType: KeyType
) : DerivationPathBuilder {

    private val coinType: CoinType = CoinType.RadixDlt

    override fun build() = DerivationPath(
        path = "$BIP44_PREFIX/44H/${coinType.value}H/${networkId.value}H/${EntityType.Account.value}H/${keyType.value}H/${accountIndex}H",
        scheme = CAP_26
    )
}

/**
 * The **default** derivation path used to derive `Identity` (Persona) keys for signing authentication,
 * at a certain (Persona) index (`ENTITY_INDEX`) and **unique per network** (`NETWORK_ID`) as per CAP-26.
 *
 * The format is:
 *      `m/44'/1022'/<NETWORK_ID>'/618'/<KEY_TYPE>'/<ENTITY_INDEX>'`
 */
private data class IdentityDerivationPathBuilder(
    private val networkId: NetworkId,
    private val identityIndex: Int,
    private val keyType: KeyType
) : DerivationPathBuilder {

    private val coinType: CoinType = CoinType.RadixDlt

    override fun build() = DerivationPath(
        path = "$BIP44_PREFIX/44H/${coinType.value}H/${networkId.value}H/${EntityType.Identity.value}H/${keyType.value}H/${identityIndex}H",
        scheme = CAP_26
    )
}

/**
 * Special purpose FactorSource ID HD Derivation path (used by all HD Factor Sources).
 *
 * The format is:
 *      `m/44'/<COIN_TYPE>'/365'`
 */
private object FactorSourceIdDerivationPathBuilder : DerivationPathBuilder {
    private val coinType: CoinType = CoinType.RadixDlt
    private const val GET_ID = 365

    override fun build() = DerivationPath(
        path = "$BIP44_PREFIX/44H/${coinType.value}H/${GET_ID}H",
        scheme = CAP_26
    )
}

/**
 * A derivation path that looks like a BIP44 path, but does not follow the BIP44 standard
 * since the last component must be hardened,
 * [contrary to the BIP44 standard](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki#user-content-Path_levels).
 * The path looks like this: `m/44'/1022'/2'/1/3'`
 *
 * It was a mistake when the Radix Olympia wallet was written, see Typescript
 * [SDK](https://github.com/radixdlt/radixdlt-javascript/blob/main/packages/crypto/src/elliptic-curve/hd/bip32/bip44/bip44.ts#L81),
 * to harden the `address_index`.
 */
private data class LegacyOlympiaBIP44LikeDerivationPathBuilder(
    private val accountIndex: Int
) : DerivationPathBuilder {

    private val coinType: CoinType = CoinType.RadixDlt

    override fun build() = DerivationPath(
        path = "$BIP44_PREFIX/44H/${coinType.value}H/0H/0/${accountIndex}H",
        scheme = CAP_26
    )
}

enum class BabylonDerivationPathComponent {
    PurposeIndex, CoinTypeIndex, NetworkIndex, EntityKindIndex, KeyKindIndex, EntityIndex
}

enum class OlympiaDerivationPathComponent {
    Purpose, CoinType, Account, Change, AddressIndex
}
