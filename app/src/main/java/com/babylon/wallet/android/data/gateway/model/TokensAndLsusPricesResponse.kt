package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.core.domain.assets.SupportedCurrency

@Serializable
data class TokensAndLsusPricesResponse(
    @SerialName(value = "tokens")
    val tokens: List<TokenPrice>,
    @SerialName(value = "lsus")
    val lsus: List<LsuPrice>
) {

    @Serializable
    data class TokenPrice(
        @SerialName(value = "resource_address")
        val resourceAddress: String,
        @SerialName(value = "usd_price")
        val usdPrice: Double,
        @SerialName(value = "last_updated_at")
        val lastUpdatedAt: String
    )

    @Serializable
    data class LsuPrice(
        @SerialName(value = "resource_address")
        val resourceAddress: String,
        @SerialName(value = "xrd_redemption_value")
        val xrdRedemptionValue: Double,
        @SerialName(value = "usd_price")
        val usdPrice: Double
    )

    companion object {

        fun TokensAndLsusPricesResponse.asEntity(): List<TokenPriceEntity> {
            val instantGenerator = InstantGenerator()
            val lsus = this.lsus.map { lsuPrice ->
                TokenPriceEntity(
                    resourceAddress = ResourceAddress.init(lsuPrice.resourceAddress),
                    price = lsuPrice.usdPrice.toDecimal192OrNull().orZero(),
                    currency = SupportedCurrency.USD.code,
                    synced = instantGenerator
                )
            }
            val tokens = this.tokens.map { tokenPrice ->
                TokenPriceEntity(
                    resourceAddress = ResourceAddress.init(tokenPrice.resourceAddress),
                    price = tokenPrice.usdPrice.toDecimal192OrNull().orZero(),
                    currency = SupportedCurrency.USD.code,
                    synced = instantGenerator
                )
            }
            return lsus + tokens
        }
    }
}

@Serializable
data class TokensPricesErrorResponse(
    val detail: String? = null
)
