package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import kotlinx.coroutines.flow.Flow
import rdx.works.core.InstantGenerator
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Dao
interface StateDao {

    @Query(
        """
        SELECT AR.account_address AS account_address, AR.amount, R.* FROM AccountResourceJoin AS AR
        INNER JOIN ResourceEntity AS R ON AR.resource_address = R.address
        WHERE AR.account_address in (:accountAddresses) AND AR.synced >= :minValidity
        """
    )
    fun observeAccountsPortfolio(
        accountAddresses: List<String>,
        minValidity: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Flow<List<AccountResourceWrapper>>

    @Query(
        """
        SELECT * FROM AccountEntity
        WHERE address in (:accountAddresses) AND progress = :withProgress AND synced >= :minValidity
        """
    )
    fun observeAccountDetails(
        accountAddresses: List<String>,
        withProgress: AccountInfoProgress = AccountInfoProgress.UPDATED,
        minValidity: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Flow<List<AccountEntity>>

    @Transaction
    fun updateAccountData(
        accountAddress: String,
        accountTypeMetadataItem: AccountTypeMetadataItem?,
        syncInfo: SyncInfo,
        accountWithResources: List<Pair<AccountResourceJoin, ResourceEntity>>,
    ) {
        insertAccountDetails(
            AccountEntity(
                address = accountAddress,
                accountType = accountTypeMetadataItem?.type,
                progress = AccountInfoProgress.PENDING,
                synced = syncInfo.synced
            )
        )
        insertResources(accountWithResources.map { it.second })
        insertAccountResourcesPortfolio(accountWithResources.map { it.first })
        changeAccountDetailsProgress(accountAddress, progress = AccountInfoProgress.UPDATED)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResources(resources: List<ResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountResourcesPortfolio(accountPortfolios: List<AccountResourceJoin>)

    @Insert(
        entity = AccountEntity::class,
        onConflict = OnConflictStrategy.REPLACE
    )
    fun insertAccountDetails(details: AccountEntity)

    @Query(
        """
        UPDATE AccountEntity 
        SET progress = :progress
        WHERE address = :accountAddresses
        """
    )
    fun changeAccountDetailsProgress(accountAddresses: String, progress: AccountInfoProgress)

    @Delete(entity = AccountEntity::class)
    fun removeAccountDetails(account: AccountEntity)

    companion object {
        val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)
    }
}
