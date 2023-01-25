package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class FungibleToken(
    val address: String,
    val totalSupply: BigDecimal,
    val totalMinted: BigDecimal,
    val totalBurnt: BigDecimal,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getImageUrl(): String? {
        return metadata[MetadataConstants.KEY_URL]
    }

    fun getTokenName(): String? {
        return metadata[MetadataConstants.KEY_NAME]
    }

    fun getTokenSymbol(): String? {
        return metadata[MetadataConstants.KEY_SYMBOL]
    }

    fun getTokenDescription(): String? {
        return metadata[MetadataConstants.KEY_DESCRIPTION]
    }

    fun getDisplayableMetadata(): Map<String, String> {
        return metadata.filterKeys {
            !MetadataConstants.SPECIAL_METADATA.contains(it)
        }
    }
}
