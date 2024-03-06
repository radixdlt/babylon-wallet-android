package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asAccountResourceJoin
import com.babylon.wallet.android.domain.model.resources.metadata.accountType
import kotlinx.coroutines.flow.Flow
import rdx.works.core.InstantGenerator
import java.math.BigDecimal
import java.time.Instant
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
            A.first_transaction_date AS first_transaction_date,
            A.state_version,
            AR.amount AS amount,
            R.*
        FROM AccountEntity AS A
        LEFT JOIN AccountResourceJoin AS AR ON A.address = AR.account_address
        LEFT JOIN ResourceEntity AS R ON AR.resource_address = R.address
        """
    )
    fun observeAccounts(): Flow<List<AccountPortfolioResponse>>

    @Query(
        """
        SELECT state_version FROM AccountEntity
        WHERE address = :accountAddress
    """
    )
    fun getAccountStateVersion(accountAddress: String): Long?

    @Query(
        """
        SELECT address, state_version FROM AccountEntity
    """
    )
    fun getAccountStateVersions(): List<AccountStateVersion>

    @Suppress("UnsafeCallOnNullableType")
    @Transaction
    fun updatePools(pools: List<PoolWithResourcesJoinResult>) {
        insertPoolDetails(pools.map { it.pool })

        val poolUnitResources = pools.map { pool ->
            pool.poolUnitResource
        }
        insertOrReplaceResources(poolUnitResources)

        val resourcesInvolvedInPools = pools.map { pool ->
            pool.resources.map { it.second }
        }.flatten()
        insertOrIgnoreResources(resourcesInvolvedInPools)

        val poolResourcesJoin = pools.map { poolResource -> poolResource.resources.map { it.first } }.flatten()
        insertPoolResources(poolResourcesJoin)

        insertDApps(pools.mapNotNull { it.associatedDApp })
        insertPoolDApp(
            pools.mapNotNull { join ->
                val dAppAddress = join.associatedDApp?.definitionAddress ?: return@mapNotNull null
                PoolDAppJoin(join.pool.address, dAppDefinitionAddress = dAppAddress)
            }
        )
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

            val accountMetadata = item.explicitMetadata?.toMetadata()
            insertAccountDetails(
                AccountEntity(
                    address = item.address,
                    accountType = accountMetadata?.accountType(),
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnoreResources(resources: List<ResourceEntity>)

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

    @Query(
        """
        UPDATE AccountEntity SET
        first_transaction_date = :firstTransactionDate
        WHERE address = :accountAddress
    """
    )
    fun updateAccountFirstTransactionDate(accountAddress: String, firstTransactionDate: Instant?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolDetails(pools: List<PoolEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolResources(poolResources: List<PoolResourceJoin>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPoolDApp(poolDApps: List<PoolDAppJoin>)

    @Query(
        """
        SELECT 
            PoolEntity.address AS pool_entity_address, 
            PoolEntity.resource_address AS pool_unit_address,
            PoolEntity.metadata AS pool_metadata,
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
            SELECT * FROM DAppEntity
            INNER JOIN PoolDAppJoin ON PoolDAppJoin.dApp_definition_address = DAppEntity.definition_address
            WHERE PoolDAppJoin.pool_address = :poolAddress
        """
    )
    fun getPoolAssociatedDApp(poolAddress: String): DAppEntity?

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

    @Query(
        """
        SELECT * FROM NFTEntity
        WHERE address = :resourceAddress AND local_id in (:localIds) and synced >= :minValidity
    """
    )
    fun getNFTDetails(resourceAddress: String, localIds: Set<String>, minValidity: Long): List<NFTEntity>?

    @Transaction
    fun storeStakeDetails(
        accountAddress: String,
        stateVersion: Long,
        lsuList: List<ResourceEntity>,
        claims: List<NFTEntity>
    ) {
        // Update NFT details
        insertNFTs(nfts = claims)
        // Inserting LSUs
        insertOrReplaceResources(lsuList)
        // Update joins
        insertAccountNFTsJoin(
            claims.map { nft ->
                AccountNFTJoin(
                    accountAddress = accountAddress,
                    resourceAddress = nft.address,
                    localId = nft.localId,
                    stateVersion = stateVersion
                )
            }
        )
    }

    @Query(
        """
        SELECT * FROM DAppEntity 
        WHERE definition_address in (:definitionAddresses)
        AND synced >= :minValidity
    """
    )
    fun getDApps(definitionAddresses: List<String>, minValidity: Long): List<DAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDApps(dApps: List<DAppEntity>)

    companion object {
        val deleteDuration = 1.toDuration(DurationUnit.SECONDS)
        private val accountsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        private val dAppsCacheDuration = 2.toDuration(DurationUnit.HOURS)
        private val resourcesCacheDuration = 48.toDuration(DurationUnit.HOURS)

        fun accountCacheValidity() = InstantGenerator().toEpochMilli() - accountsCacheDuration.inWholeMilliseconds
        fun resourcesCacheValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else resourcesCacheDuration.inWholeMilliseconds

        fun dAppsCacheValidity() = InstantGenerator().toEpochMilli() - dAppsCacheDuration.inWholeMilliseconds
    }
}
