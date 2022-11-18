package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import java.math.BigDecimal

data class FungibleToken(
    val address: String,
    val totalSupply: BigDecimal,
    val totalMinted: BigDecimal,
    val totalBurnt: BigDecimal,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getImageUrl(): String? {
        return metadata[TokenMetadataConstants.KEY_URL]
    }

    fun getTokenName(): String? {
        return metadata[TokenMetadataConstants.KEY_NAME]
    }

    fun getTokenSymbol(): String? {
        return metadata[TokenMetadataConstants.KEY_SYMBOL]
    }

    fun getTokenDescription(): String? {
        return metadata[TokenMetadataConstants.KEY_DESCRIPTION]
    }
}

fun EntityDetailsResponse.toFungibleToken(): FungibleToken {
    return FungibleToken(
        address = address,
        totalSupply = BigDecimal(details.totalSupply?.value),
        totalMinted = BigDecimal(details.totalMinted?.value),
        totalBurnt = BigDecimal(details.totalBurnt?.value),
        metadata = metadata.items.associate { it.key to it.value }
    )
}