package rdx.works.profile.derivation.model

/**
 * Key types defined as per CAP-26
 * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2897772650/CAP-26+SLIP10+HD+Derivation+Path+Scheme
 */
@Suppress("MagicNumber")
enum class KeyType(val value: Int) {
    // Key to be used for signing transactions.
    TRANSACTION_SIGNING(1460),

    // Key to be used for signing authentication.
    AUTHENTICATION_SIGNING(1678),

    // Key to be used for encrypting messages.
    MESSAGE_ENCRYPTION(1391)
}
