package rdx.works.profile.derivation.model

/**
 * Currently we only support Radix coin which is documented here -> https://github.com/satoshilabs/slips/pull/1137
 */
@Suppress("MagicNumber")
enum class CoinType(val value: Int) {
    RadixDlt(1022)
}
