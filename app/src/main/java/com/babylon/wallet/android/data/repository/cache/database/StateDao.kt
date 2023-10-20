package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Dao
interface StateDao {

    @Query(
        """
        SELECT AR.account_address AS account_address, AR.amount, AR.synced AS amount_synced, AR.epoch AS amount_epoch, R.* FROM AccountResourcesPortfolio AS AR
        INNER JOIN ResourceEntity AS R ON AR.resource_address = R.address
        WHERE AR.account_address in (:accountAddresses)
    """
    )
    fun observeAccountsPortfolio(
        accountAddresses: List<String>,
//        minValidity: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Flow<List<AccountResourceWrapper>>

    @Query(
        """
        SELECT * FROM AccountDetailsEntity
        WHERE address in (:accountAddresses)
    """
    )
    fun observeAccountDetails(accountAddresses: List<String>): Flow<List<AccountDetailsEntity>>

    @Transaction
    fun updateAccountData(
        accountDetails: AccountDetailsEntity,
        accountWithResources: List<Pair<AccountResourcesPortfolio, ResourceEntity>>,
    ) {
        insertAccountDetails(accountDetails)
        insertResources(accountWithResources.map { it.second })
        insertAccountResourcesPortfolio(accountWithResources.map { it.first })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResources(resources: List<ResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountResourcesPortfolio(accountPortfolios: List<AccountResourcesPortfolio>)

    @Insert(
        entity = AccountDetailsEntity::class,
        onConflict = OnConflictStrategy.REPLACE
    )
    fun insertAccountDetails(account: AccountDetailsEntity)

    @Delete(entity = AccountDetailsEntity::class)
    fun removeAccountDetails(account: AccountDetailsEntity)

    companion object {
        val accountsCacheDuration = 2.toDuration(DurationUnit.SECONDS)
        val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)
    }
}
