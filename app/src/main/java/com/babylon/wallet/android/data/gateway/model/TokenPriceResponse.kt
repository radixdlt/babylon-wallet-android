package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.data.repository.cache.database.TokenPriceEntity
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
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
                    resourceAddress = ResourceAddress.init(tokenPrice.resourceAddress),
                    price = tokenPrice.price,
                    currency = tokenPrice.currency,
                    synced = InstantGenerator()
                )
            }
        }
    }
}
