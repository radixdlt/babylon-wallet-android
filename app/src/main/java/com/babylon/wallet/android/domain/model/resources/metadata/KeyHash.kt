package com.babylon.wallet.android.domain.model.resources.metadata

sealed interface KeyHash {
    val hex: String

    data class EcdsaSecp256k1(
        override val hex: String
    ) : KeyHash

    data class EddsaEd25519(
        override val hex: String
    ) : KeyHash
}
