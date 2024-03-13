package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.isXrd

class TokensPriceSorter(
    private val pricesPerAsset: Map<Asset, AssetPrice?>?
) : Comparator<Token> {
    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    override fun compare(thisToken: Token?, otherToken: Token?): Int {
        return when {
            thisToken == null && otherToken == null -> 0
            thisToken != null && otherToken == null -> -1
            thisToken == null && otherToken != null -> 1
            else -> {
                requireNotNull(thisToken)
                requireNotNull(otherToken)

                if (pricesPerAsset.isNullOrEmpty()) {
                    return thisToken.resource.compareTo(otherToken.resource)
                }

                if (thisToken.resource.isXrd) {
                    return -1
                } else if (otherToken.resource.isXrd) {
                    return 1
                }

                val thisPrice = pricesPerAsset[thisToken]?.price
                val otherPrice = pricesPerAsset[otherToken]?.price

                when {
                    thisPrice == null && otherPrice == null -> thisToken.resource.compareTo(otherToken.resource)
                    thisPrice != null && otherPrice == null -> -1
                    thisPrice == null && otherPrice != null -> 1
                    else -> {
                        requireNotNull(thisPrice)
                        requireNotNull(otherPrice)

                        // Comparison between amounts is done in descending order
                        otherPrice.compareTo(thisPrice)
                    }
                }
            }
        }
    }
}
