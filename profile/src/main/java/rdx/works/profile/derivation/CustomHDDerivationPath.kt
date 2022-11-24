package rdx.works.profile.derivation

import com.radixdlt.bip44.BIP44
import com.radixdlt.bip44.BIP44_PREFIX
import rdx.works.profile.derivation.model.CoinType

/**
 * A custom derivation path used to derive keys for whatever purpose. [CAP-26][cap26] states
 * The format is:
 *          `m/44'/1022'`
 * Where `'` denotes hardened path, which is **required** as per [SLIP-10][slip10].
 */
data class CustomHDDerivationPath(
    val bip44: BIP44
) {
    private val coinType: CoinType = CoinType.RadixDlt

    /**
     * Check if path starts with m/44'/1022' (hardened or not). Otherwise throw exception
     */
    val path: String
        get() = if (
            bip44.toString().startsWith("$BIP44_PREFIX/44'/${coinType.value}") ||
            bip44.toString().startsWith("$BIP44_PREFIX/44/${coinType.value}")
        ) bip44.toString() else throw IllegalArgumentException("Invalid derivation path")

}
