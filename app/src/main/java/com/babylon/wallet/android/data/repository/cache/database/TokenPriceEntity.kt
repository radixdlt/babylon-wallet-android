package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import java.math.BigDecimal
import java.time.Instant

@Entity(primaryKeys = ["resource_address", "currency"])
data class TokenPriceEntity(
    @ColumnInfo("resource_address")
    val resourceAddress: ResourceAddress,
    val price: BigDecimal,
    val currency: String,
    val synced: Instant
) {
    companion object {
        fun List<TokenPriceEntity>.toFiatPrices(): Map<ResourceAddress, FiatPrice> = associate {
            it.resourceAddress to FiatPrice(
                price = it.price.toDouble(),
                currency = SupportedCurrency.fromCode(it.currency) ?: SupportedCurrency.USD
            )
        }
    }
}
