package com.babylon.wallet.android.domain.model

data class NonFungibleToken(
    val address: String,
    val tokenItems: List<NonFungibleTokenItemContainer>,
    val metadataContainer: NonFungibleMetadataContainer? = null
) {
    fun getTokenName(): String? {
        return metadataContainer?.metadata?.get(MetadataConstants.KEY_NAME)
    }

    fun getIconUrl(): String? {
        return metadataContainer?.metadata?.get(MetadataConstants.KEY_ICON)
    }
}
