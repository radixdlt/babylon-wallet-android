package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.InstantGenerator
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Dao
interface TokenPriceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokensPrice(tokensPrices: List<TokenPriceEntity>)

    @Query("SELECT * FROM TokenPriceEntity WHERE resource_address in (:addresses) AND synced >= :minValidity")
    suspend fun getTokensPrices(
        addresses: Set<ResourceAddress>,
        minValidity: Long
    ): List<TokenPriceEntity>

    companion object {
        private val tokenPriceCacheDuration = 5.toDuration(DurationUnit.MINUTES)

        fun tokenPriceCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else tokenPriceCacheDuration.inWholeMilliseconds
    }
}
