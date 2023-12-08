package com.babylon.wallet.android.domain.model.resources.metadata

data class PublicKeyHashes(
    val keys: List<PublicKeyHash>,
    val lastUpdatedAtStateVersion: Long
)

sealed interface PublicKeyHash {
    val hex: String
    val lastUpdatedAtStateVersion: Long

    data class EcdsaSecp256k1(
        override val hex: String,
        override val lastUpdatedAtStateVersion: Long
    ) : PublicKeyHash

    data class EddsaEd25519(
        override val hex: String,
        override val lastUpdatedAtStateVersion: Long
    ) : PublicKeyHash
}
