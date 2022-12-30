package rdx.works.profile.derivation.model

/**
 * Key types defined as per cap-26 -> https://radixdlt.atlassian.net/wiki/spaces/~5c1cdd7dad984b5210851e01/pages/
 * 2897772650/CAP-26+SLIP10+HD+Derivation+Path+Scheme
 * SignTransaction used 1238 cause its ASCII sum of "SIGN_TRANSACTION"
 * SignAuth used 706 cause its ASCII sum of "SIGN_AUTH"
 */
@Suppress("MagicNumber")
enum class KeyType(val value: Int) {
    // Key to be used for signing transactions.
    SignTransaction(1238),

    // Key to be used for signing authentication.
    SignAuth(706)
}
