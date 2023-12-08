package com.babylon.wallet.android.domain.model.resources.metadata

sealed interface PublicKeyHash {
    val hex: String

    data class EcdsaSecp256k1(
        override val hex: String
    ) : PublicKeyHash

    data class EddsaEd25519(
        override val hex: String
    ) : PublicKeyHash
}
