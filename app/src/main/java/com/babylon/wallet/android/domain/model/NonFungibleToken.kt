package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse

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

fun EntityDetailsResponse.toNonFungibleToken(): NonFungibleToken {
    return NonFungibleToken(
        address = address,
    )
}
