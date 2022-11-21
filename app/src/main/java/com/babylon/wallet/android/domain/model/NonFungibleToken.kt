package com.babylon.wallet.android.domain.model

data class NonFungibleToken(
    val address: String,
    val nonFungibleID: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getTokenName(): String? {
        return metadata[TokenMetadataConstants.KEY_NAME]
    }

    fun getTokenSymbol(): String? {
        return metadata[TokenMetadataConstants.KEY_SYMBOL]
    }

    fun getImageUrl(): String? {
        return metadata[TokenMetadataConstants.KEY_URL]
    }
}
