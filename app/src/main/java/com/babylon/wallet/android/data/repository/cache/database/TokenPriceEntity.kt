package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.domain.model.assets.TokenPrice
import java.math.BigDecimal
import java.time.Instant

@Entity(primaryKeys = ["resource_address", "currency"])
data class TokenPriceEntity(
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    val price: BigDecimal,
    val currency: String,
    val synced: Instant
) {
    companion object {

        fun List<TokenPriceEntity>.toTokenPrice(): List<TokenPrice> {
            return map { tokenPriceEntity ->
                TokenPrice(
                    resourceAddress = tokenPriceEntity.resourceAddress,
                    price = tokenPriceEntity.price,
                    currency = tokenPriceEntity.currency
                )
            }
        }
    }
}
