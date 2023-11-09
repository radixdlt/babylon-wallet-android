package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asAccountResourceJoin
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import kotlinx.coroutines.flow.Flow
import rdx.works.core.InstantGenerator
import java.math.BigDecimal
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("TooManyFunctions") // TODO Improve DAO separation
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
            R.*
        FROM AccountEntity AS A
        LEFT JOIN AccountResourceJoin AS AR ON A.address = AR.account_address
        LEFT JOIN ResourceEntity AS R ON AR.resource_address = R.address
        """
    )
    fun observeAccounts(): Flow<List<AccountPortfolioResponse>>

    @Query("""
        SELECT state_version FROM AccountEntity
        WHERE address = :accountAddress
    """)
    fun getAccountStateVersion(accountAddress: String): Long?

    @Suppress("UnsafeCallOnNullableType")
    @Transaction
    fun updatePools(pools: Map<ResourceEntity, List<Pair<PoolResourceJoin, ResourceEntity>>>) {
        val poolEntities = pools.keys.map {
            PoolEntity(
                address = it.poolAddress!!,
                resourceAddress = it.address
            )
        }
        insertPoolDetails(poolEntities)

        val resourcesInvolved = pools.map { entry -> listOf(entry.key) + entry.value.map { it.second } }.flatten()
        insertOrReplaceResources(resourcesInvolved)
        val join = pools.values.map { poolResource -> poolResource.map { it.first } }.flatten()
        insertPoolResources(join)
    }

    @Transaction
    fun updateAccountData(accountsGatewayDetails: List<Pair<StateEntityDetailsResponseItem, LedgerState>>) {
        accountsGatewayDetails.forEach { pair ->
            val item = pair.first
            val ledgerState = pair.second

            val syncInfo = SyncInfo(synced = InstantGenerator(), accountStateVersion = ledgerState.stateVersion)
            val allResources = item.fungibleResources?.items?.map { fungibleItem ->
                fungibleItem.asAccountResourceJoin(item.address, syncInfo)
            }.orEmpty() + item.nonFungibleResources?.items?.map { nonFungibleItem ->
                nonFungibleItem.asAccountResourceJoin(item.address, syncInfo)
            }.orEmpty()

            val accountMetadataItems = item.explicitMetadata?.asMetadataItems()?.toMutableList()
            insertAccountDetails(
                AccountEntity(
                    address = item.address,
                    accountType = accountMetadataItems?.consume<AccountTypeMetadataItem>()?.type,
                    synced = syncInfo.synced,
                    stateVersion = syncInfo.accountStateVersion
                )
            )
            insertOrReplaceResources(allResources.map { it.second })
            insertAccountResourcesPortfolio(allResources.map { it.first })
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceResources(resources: List<ResourceEntity>)

    @Query(
        """
        UPDATE ResourceEntity SET
        divisibility = :divisibility,
        behaviours = :behaviours,
        supply = :supply
        WHERE address = :resourceAddress
    """
    )
    fun updateResourceEntity(resourceAddress: String, divisibility: Int?, behaviours: BehavioursColumn?, supply: BigDecimal?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountResourcesPortfolio(accountPortfolios: List<AccountResourceJoin>)

    @Insert(
        entity = AccountEntity::class,
        onConflict = OnConflictStrategy.REPLACE
    )
    fun insertAccountDetails(details: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolDetails(pools: List<PoolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolResources(poolResources: List<PoolResourceJoin>)

    @Query(
        """
        SELECT 
            PoolEntity.address AS pool_entity_address, 
            PoolResourceJoin.state_version AS account_state_version, 
            PoolResourceJoin.amount AS amount,
            ResourceEntity.*
        FROM PoolEntity
        LEFT JOIN PoolResourceJoin ON PoolEntity.address = PoolResourceJoin.pool_address
        LEFT JOIN ResourceEntity ON PoolResourceJoin.resource_address = ResourceEntity.address
        WHERE PoolEntity.address IN (:addresses) AND account_state_version = :atStateVersion
    """
    )
    fun getPoolDetails(addresses: Set<String>, atStateVersion: Long): List<PoolWithResourceResponse>

    @Query(
        """
        SELECT RE.* FROM ResourceEntity AS RE
        LEFT JOIN PoolEntity ON PoolEntity.resource_address = RE.address
        WHERE RE.pool_address = :poolAddress AND RE.divisibility IS NOT NULL AND RE.supply IS NOT NULL AND RE.synced >= :minValidity
    """
    )
    fun getPoolResource(poolAddress: String, minValidity: Long): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertValidators(validators: List<ValidatorEntity>)

    @Query(
        """
        SELECT * FROM ValidatorEntity
        WHERE address in (:addresses) AND state_version = :atStateVersion
    """
    )
    fun getValidators(addresses: Set<String>, atStateVersion: Long): List<ValidatorEntity>

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

    @Query(
        """
        UPDATE AccountResourceJoin
        SET next_cursor = :cursor
        WHERE AccountResourceJoin.account_address = :accountAddress AND AccountResourceJoin.resource_address = :resourceAddress
    """
    )
    fun updateNextCursor(accountAddress: String, resourceAddress: String, cursor: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccountNFTsJoin(accountNFTsJoin: List<AccountNFTJoin>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNFTs(nfts: List<NFTEntity>)

    @Transaction
    fun insertAccountNFTsJoin(
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

    @Query(
        """
        SELECT * FROM ResourceEntity
        WHERE address = :resourceAddress AND synced >= :minValidity
    """
    )
    fun getResourceDetails(resourceAddress: String, minValidity: Long): ResourceEntity?

    @Query(
        """
        SELECT * FROM AccountResourceJoin
        WHERE 
            account_address = :accountAddress AND 
            resource_address = :resourceAddress AND
            state_version = (SELECT state_version FROM AccountEntity WHERE address = :accountAddress)
    """
    )
    fun getAccountResourceJoin(resourceAddress: String, accountAddress: String): AccountResourceJoin?

    @Query(
        """
        SELECT * FROM NFTEntity
        WHERE address = :resourceAddress AND local_id = :localId and synced >= :minValidity
    """
    )
    fun getNFTDetails(resourceAddress: String, localId: String, minValidity: Long): NFTEntity?

    @Transaction
    fun storeStakeDetails(
        stakeResourceEntity: ResourceEntity?,
        claims: List<NFTEntity>?
    ) {
        stakeResourceEntity?.let {
            insertOrReplaceResources(listOf(stakeResourceEntity))
        }
        claims?.let { insertNFTs(it) }
    }

    companion object {
        private val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        private val resourcesCacheDuration = 48.toDuration(DurationUnit.HOURS)

        fun accountCacheValidity() = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
        fun resourcesCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else resourcesCacheDuration.inWholeMilliseconds
    }
}
