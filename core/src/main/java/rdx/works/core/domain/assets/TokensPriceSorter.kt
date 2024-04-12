package rdx.works.core.domain.assets

import com.radixdlt.sargon.extensions.compareTo

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

                        if (thisPrice.currency != otherPrice.currency) {
                            error("Cannot compare different currencies. Comparison of ${thisPrice.currency} with ${otherPrice.currency}")
                        }

                        // Comparison between amounts is done in descending order
                        otherPrice.price.compareTo(thisPrice.price)
                    }
                }
            }
        }
    }
}
