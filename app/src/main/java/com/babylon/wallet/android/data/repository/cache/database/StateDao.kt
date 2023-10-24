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

    @Insert(
        entity = StateVersionEntity::class,
        onConflict = OnConflictStrategy.REPLACE
    )
    fun insertStateVersion(stateVersionEntity: StateVersionEntity)

    @Query(
        """
        SELECT 
            A.address AS account_address, 
            A.account_type AS account_type, 
            A.synced AS account_synced, 
            A.state_version AS account_state_version, 
            A.progress, 
            AR.amount AS amount,
            R.address AS resource_address,
            R.*
        FROM AccountEntity AS A
        LEFT JOIN AccountResourceJoin AS AR ON A.address = AR.account_address
        LEFT JOIN ResourceEntity AS R ON AR.resource_address = R.address
        WHERE 
            A.address in (:accountAddresses) AND 
            A.progress = 'UPDATED' AND 
            A.synced >= :minValidity AND  
            A.state_version = (SELECT version FROM StateVersionEntity WHERE id = 1)
        """
    )
    fun observeAccountsPortfolio(
        accountAddresses: List<String>,
        minValidity: Long = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
    ): Flow<List<AccountPortfolioResponse>>

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
                synced = syncInfo.synced,
                stateVersion = syncInfo.stateVersion
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

    @Transaction
    fun insertPools(
        poolsWithResources: Map<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>>
    ) {
        insertPoolDetails(poolsWithResources.keys.toList())
        val allValues = poolsWithResources.values.flatten()
        insertResources(allValues.map { it.second })
        insertPoolResources(allValues.map { it.first })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolDetails(pools: List<PoolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolResources(poolResources: List<PoolResourceJoin>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertValidators(validators: List<ValidatorEntity>)

    companion object {
        val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)
    }
}
