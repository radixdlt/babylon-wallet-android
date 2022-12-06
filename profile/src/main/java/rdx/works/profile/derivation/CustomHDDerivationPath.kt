package rdx.works.profile.derivation

import com.radixdlt.bip44.BIP44
import com.radixdlt.bip44.BIP44Element
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
        get() {
            require(
                value =
                bip44.toString().startsWith("$BIP44_PREFIX/44'/${coinType.value}") ||
                    bip44.toString().startsWith("$BIP44_PREFIX/44/${coinType.value}")
            ) { "Invalid derivation path" }
            return bip44.toString()
        }

    companion object {
        /**
         * https://radixdlt.atlassian.net/wiki/spaces/~5c1cdd7dad984b5210851e01/pages/2897772650/
         * CAP-26+SLIP10+HD+Derivation+Path+Scheme
         */
        private const val GET_ID = 365

        val getId = CustomHDDerivationPath(
            bip44 = BIP44(
                path = listOf(
                    BIP44Element(
                        hardened = true,
                        number = 44
                    ),
                    BIP44Element(
                        hardened = true,
                        number = CoinType.RadixDlt.value
                    ),
                    BIP44Element(
                        hardened = true,
                        number = GET_ID
                    )
                )
            )
        )
    }
}
