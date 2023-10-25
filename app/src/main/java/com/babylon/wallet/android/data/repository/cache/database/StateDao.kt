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
        SELECT 
            A.address AS account_address, 
            A.account_type AS account_type, 
            A.synced AS account_synced, 
            A.state_version AS account_state_version,
            AR.amount AS amount,
            R.address AS resource_address,
            R.*
        FROM AccountEntity AS A
        LEFT JOIN AccountResourceJoin AS AR ON A.address = AR.account_address
        LEFT JOIN ResourceEntity AS R ON AR.resource_address = R.address
        WHERE 
            A.address in (:accountAddresses) AND
            A.synced >= :minValidity
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
        poolsWithResources: Map<PoolEntity, List<Pair<PoolResourceJoin, ResourceEntity>>>,
        validators: List<ValidatorEntity>
    ) {
        val allPoolsWithResources = poolsWithResources.values.flatten()

        // Insert parent entities
        insertAccountDetails(
            AccountEntity(
                address = accountAddress,
                accountType = accountTypeMetadataItem?.type,
                synced = syncInfo.synced,
                stateVersion = syncInfo.stateVersion
            )
        )
        insertPoolDetails(poolsWithResources.keys.toList())
        insertValidators(validators)
        insertResources(accountWithResources.map { it.second } + allPoolsWithResources.map { it.second })

        // Insert joins
        insertAccountResourcesPortfolio(accountWithResources.map { it.first })
        insertPoolResources(allPoolsWithResources.map { it.first })
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

    @Delete(entity = AccountEntity::class)
    fun removeAccountDetails(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolDetails(pools: List<PoolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolResources(poolResources: List<PoolResourceJoin>)

    @Query("""
        SELECT 
            PoolEntity.address AS pool_entity_address, 
            PoolEntity.state_version AS pool_state_version, 
            PoolResourceJoin.amount AS amount,
            ResourceEntity.*
        FROM PoolEntity
        LEFT JOIN PoolResourceJoin ON PoolEntity.address IN (:addresses) AND PoolEntity.state_version = :atStateVersion
        LEFT JOIN ResourceEntity ON PoolResourceJoin.resource_address = ResourceEntity.address
    """)
    fun getPoolDetails(addresses: Set<String>, atStateVersion: Long): List<PoolWithResourceResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertValidators(validators: List<ValidatorEntity>)

    @Query("""
        SELECT * FROM ValidatorEntity
        WHERE address in (:addresses) AND state_version = :atStateVersion
    """)
    fun getValidators(addresses: Set<String>, atStateVersion: Long): List<ValidatorEntity>

    companion object {
        val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        val resourcesCacheDuration = 24.toDuration(DurationUnit.HOURS)
    }
}
