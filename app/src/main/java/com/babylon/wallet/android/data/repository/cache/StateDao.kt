package com.babylon.wallet.android.data.repository.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Dao
interface StateDao {

    @Query(
        "SELECT * FROM OwnedFungibleEntity " +
        "INNER JOIN FungibleResourceEntity ON OwnedFungibleEntity.resource_address = FungibleResourceEntity.address " +
        "WHERE OwnedFungibleEntity.account_address IN (:accountAddresses)"
    )
    fun getAccountFungibles(
        accountAddresses: List<String>,
//        syncedUntil: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Map<OwnedFungibleEntity, FungibleResourceEntity>

    @Query(
        "SELECT * FROM OwnedNonFungibleEntity " +
        "INNER JOIN NonFungibleResourceEntity ON OwnedNonFungibleEntity.resource_address = NonFungibleResourceEntity.address " +
        "WHERE OwnedNonFungibleEntity.account_address IN (:accountAddresses)"
    )
    fun getAccountNonFungibles(
        accountAddresses: List<String>,
//        syncedUntil: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Map<OwnedNonFungibleEntity, NonFungibleResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFungibles(fungibles: List<FungibleResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNonFungibles(fungibles: List<NonFungibleResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOwnedFungibles(accounts: List<OwnedFungibleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOwnedNonFungibles(accounts: List<OwnedNonFungibleEntity>)

    companion object {
        val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)
    }
}
