package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator

@Serializable
data class TokenPriceResponse(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "resource_address")
    val resourceAddress: String,
    @SerialName(value = "symbol")
    val symbol: String,
    @SerialName(value = "name")
    val name: String,
    @SerialName(value = "price")
    val price: Double,
    @SerialName(value = "currency")
    val currency: String
) {

    companion object {

        fun List<TokenPriceResponse>.asEntities(): List<TokenPriceEntity> {
            return map { tokenPrice ->
                TokenPriceEntity(
                    resourceAddress = tokenPrice.resourceAddress,
                    price = tokenPrice.price.toBigDecimal(),
                    currency = tokenPrice.currency,
                    synced = InstantGenerator()
                )
            }
        }
    }
}
