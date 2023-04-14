package rdx.works.profile.derivation

import com.radixdlt.bip44.BIP44_PREFIX
import rdx.works.profile.derivation.model.CoinType

/**
 *  A derivation path that looks like a BIP44 path, but does not follow the BIP44 standard
 *  since the last component must be hardened, [contrary to the BIP44 standard][bip44]. The
 *  path looks like this: `m/44'/1022'/2'/1/3'`
 *  It was a mistake by me (Alexander Cyon) when I wrote the Radix Olympia wallet, [see Typescript SDK][radixJS], to
 *  harden the `address_index`.
 *
 * [cap26]: https://radixdlt.atlassian.net/l/cp/UNaBAGUC
 * [radixJS]: https://github.com/radixdlt/radixdlt-javascript/blob/main/packages/crypto/src/elliptic-curve/hd/bip32/bip44/bip44.ts#L81
 * [bip44]: https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki#user-content-Path_levels
 */
data class LegacyOlympiaBIP44LikeDerivationPath(private val accountIndex: Int) {
    private val coinType: CoinType = CoinType.RadixDlt

    val path: String
        get() = "$BIP44_PREFIX/44H/${coinType.value}H/0H/0/${accountIndex}H"
}
