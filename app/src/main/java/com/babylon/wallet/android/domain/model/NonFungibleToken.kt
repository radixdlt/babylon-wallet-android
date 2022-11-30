package com.babylon.wallet.android.domain.model

data class NonFungibleToken(
    val address: String,
    val nonFungibleIdContainer: NonFungibleTokenIdContainer? = null,
    val metadataContainer: NonFungibleMetadataContainer? = null
) {
    fun getTokenName(): String? {
        return metadataContainer?.metadata?.get(TokenMetadataConstants.KEY_NAME)
    }

    fun getTokenSymbol(): String? {
        return metadataContainer?.metadata?.get(TokenMetadataConstants.KEY_SYMBOL)
    }

    fun getImageUrl(): String? {
        return metadataContainer?.metadata?.get(TokenMetadataConstants.KEY_NFT_IMAGE)
    }
}
