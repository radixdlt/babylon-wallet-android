package rdx.works.profile.derivation.model

enum class KeyType(val value: Int) {
    // Key to be used for signing transactions.
    SignTransaction(1238),
    // Key to be used for signing authentication.
    SignAuth(706)
}