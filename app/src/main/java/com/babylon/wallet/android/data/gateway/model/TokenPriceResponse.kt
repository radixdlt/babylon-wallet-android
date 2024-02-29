package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator

@Serializable
data class TokenPriceResponse(
    val id: String,
    @SerialName(value = "resource_address")
    val resourceAddress: String,
    val symbol: String,
    val name: String,
    val price: Double,
    val currency: String
) {

    companion object {

        fun List<TokenPriceResponse>.asEntity(): List<TokenPriceEntity> {
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
