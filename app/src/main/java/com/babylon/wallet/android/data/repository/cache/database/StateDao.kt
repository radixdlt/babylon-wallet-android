package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import kotlinx.coroutines.flow.Flow

@Dao
interface StateDao {

    @Query(
        """
        SELECT 
            A.address AS account_address, 
            A.account_type AS account_type, 
            A.synced AS account_synced, 
            A.state_version,
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
        minValidity: Long
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
                synced = syncInfo.synced,
                stateVersion = syncInfo.accountStateVersion
            )
        )
        insertResources(accountWithResources.map { it.second })
        insertAccountResourcesPortfolio(accountWithResources.map { it.first })
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
            PoolResourceJoin.state_version AS account_state_version, 
            PoolResourceJoin.amount AS amount,
            ResourceEntity.*
        FROM PoolEntity
        LEFT JOIN PoolResourceJoin ON PoolEntity.address = PoolResourceJoin.pool_address
        LEFT JOIN ResourceEntity ON PoolResourceJoin.resource_address = ResourceEntity.address
        WHERE PoolEntity.address IN (:addresses) AND account_state_version = :atStateVersion
    """)
    fun getPoolDetails(addresses: Set<String>, atStateVersion: Long): List<PoolWithResourceResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertValidators(validators: List<ValidatorEntity>)

    @Query("""
        SELECT * FROM ValidatorEntity
        WHERE address in (:addresses) AND state_version = :atStateVersion
    """)
    fun getValidators(addresses: Set<String>, atStateVersion: Long): List<ValidatorEntity>

    @Query("""
        SELECT ARJ.account_address, ARJ.resource_address, ARJ.vault_address, ARJ.next_cursor, AccountEntity.state_version 
        FROM AccountResourceJoin AS ARJ
        INNER JOIN AccountEntity ON ARJ.account_address = AccountEntity.address
        WHERE ARJ.account_address = :accountAddress AND ARJ.resource_address = :resourceAddress
    """)
    fun getAccountNFTPortfolio(accountAddress: String, resourceAddress: String): List<AccountOnNonFungibleCollectionStateResponse>

    @Query(
        """
        SELECT NFTEntity.* FROM AccountNFTJoin
        INNER JOIN NFTEntity ON AccountNFTJoin.resource_address = NFTEntity.address AND AccountNFTJoin.local_id = NFTEntity.local_id
        WHERE 
            AccountNFTJoin.account_address = :accountAddress AND 
            AccountNFTJoin.resource_address = :resourceAddress AND 
            AccountNFTJoin.state_version = :stateVersion
        """
    )
    fun getOwnedNfts(accountAddress: String, resourceAddress: String, stateVersion: Long): List<NFTEntity>

    @Query("""
        UPDATE AccountResourceJoin
        SET next_cursor = :cursor
        WHERE AccountResourceJoin.account_address = :accountAddress AND AccountResourceJoin.resource_address = :resourceAddress
    """)
    fun updateNextCursor(accountAddress: String, resourceAddress: String, cursor: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountNFTsJoin(accountNFTsJoin: List<AccountNFTJoin>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNFTs(nfts: List<NFTEntity>)

    @Transaction
    fun storeAccountNFTsPortfolio(
        accountAddress: String,
        resourceAddress: String,
        cursor: String?,
        nfts: List<NFTEntity>,
        accountNFTsJoin: List<AccountNFTJoin>,
    ) {
        updateNextCursor(accountAddress, resourceAddress, cursor)
        insertNFTs(nfts)
        insertAccountNFTsJoin(accountNFTsJoin)
    }

    @Query("""
        SELECT * FROM ResourceEntity
        WHERE address = :resourceAddress AND synced >= :minValidity
    """)
    fun getResourceDetails(resourceAddress: String, minValidity: Long): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateResourceDetails(entity: ResourceEntity)

    @Query("""
        SELECT * FROM AccountResourceJoin
        WHERE account_address = :accountAddress AND resource_address = :resourceAddress
    """)
    fun getAccountResourceJoin(resourceAddress: String, accountAddress: String): AccountResourceJoin?

    @Query("""
        SELECT * FROM NFTEntity
        WHERE address = :resourceAddress AND local_id = :localId and synced >= :minValidity
    """)
    fun getNFTDetails(resourceAddress: String, localId: String, minValidity: Long): NFTEntity?
}
