package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.fetchAccountGatewayDetails
import com.babylon.wallet.android.data.gateway.extensions.fetchPools
import com.babylon.wallet.android.data.gateway.extensions.fetchValidators
import com.babylon.wallet.android.data.gateway.extensions.fetchVaultDetails
import com.babylon.wallet.android.data.repository.cache.database.AccountPortfolioResponse
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPoolsResourcesJoin
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.accountCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.StateDatabase
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntities
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidators
import com.babylon.wallet.android.data.repository.cache.database.getCachedPools
import com.babylon.wallet.android.data.repository.cache.database.getCachedValidators
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.core.toUnitResult
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountsStateCache @Inject constructor(
    private val api: StateApi,
    private val dao: StateDao,
    private val database: StateDatabase,
    private val getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val accountsMemoryCache = MutableStateFlow<List<AccountAddressWithAssets>?>(null)
    private val cacheErrors = MutableStateFlow<Throwable?>(null)
    private val accountsRequested = MutableStateFlow<Set<String>>(emptySet())
    private val deletingDatabaseMutex = Mutex()

    init {
        dao.observeAccounts()
            .collectCachedData()
            .onEach { logger.d("Cache invoked") }
            .compileAccountAddressAssets()
            .distinctUntilChanged()
            .onEach { accountsMemoryCache.value = it }
            .launchIn(applicationScope)
    }

    fun observeAccountsOnLedger(
        accounts: List<Network.Account>,
        isRefreshing: Boolean
    ): Flow<List<AccountWithAssets>> = combineTransform(
        accountsMemoryCache,
        cacheErrors
    ) { cache, error ->
        if (error != null) {
            cacheErrors.value = null
            throw error
        }

        emit(cache)
    }.filterNotNull()
        .onStart {
            if (isRefreshing) {
                logger.d("\uD83D\uDD04 Refreshing accounts")
                fetchAllResources(accounts.map { it.address }.toSet())
            }
        }
        .transform { cachedAccounts ->
            val accountsToReturn = accounts.map { account ->
                cachedAccounts.find { it.address == account.address }?.toAccountWithAssets(
                    account = account,
                    dao = dao
                ) ?: AccountWithAssets(account = account)
            }
            emit(accountsToReturn)

            // Do not request accounts while deleting cache
            if (deletingDatabaseMutex.isLocked) return@transform

            val knownStateVersion = accountsToReturn.maxOfOrNull { it.details?.stateVersion ?: -1L }?.takeIf { it > 0L }
            val accountsToRequest = accountsToReturn.filter { it.assets == null }.map { it.account.address }.toSet()
            fetchAllResources(
                accountAddresses = accountsToRequest,
                onStateVersion = knownStateVersion,
            )
        }
        .flowOn(dispatcher)

    suspend fun getOwnedXRD(accounts: List<Network.Account>) = withContext(dispatcher) {
        if (accounts.isEmpty()) return@withContext Result.success(emptyMap())

        val xrdAddress = XrdResource.address(networkId = accounts.first().networkID)

        val accountsWithXRDVaults = accounts.associateWith { account ->
            dao.getAccountResourceJoin(accountAddress = account.address, resourceAddress = xrdAddress)?.vaultAddress
        }

        runCatching {
            val vaultsWithAmounts = api.fetchVaultDetails(accountsWithXRDVaults.mapNotNull { it.value }.toSet())

            accountsWithXRDVaults.mapValues { entry ->
                entry.value?.let { vaultsWithAmounts[it] } ?: BigDecimal.ZERO
            }
        }
    }

    suspend fun clear() = runCatching {
        deletingDatabaseMutex.lock()
        withContext(dispatcher) {
            database.clearAllTables()
        }
        applicationScope.launch {
            // observeAccountsOnLedger will emit immediately after the db has been deleted
            // and it may have an active connection of a several amount of accounts
            // We need to delay a bit so the subscriber (view model) can be killed by the system
            delay(StateDao.deleteDuration)
            deletingDatabaseMutex.unlock()
        }
    }.toUnitResult()

    private suspend fun fetchAllResources(
        accountAddresses: Set<String>,
        onStateVersion: Long? = null,
    ) {
        val addressesInProgress = accountsRequested.getAndUpdate { value -> value union accountAddresses }
        val accountsToRequest = accountAddresses subtract addressesInProgress
        if (accountsToRequest.isEmpty()) return

        logger.d("☁️ ${accountsToRequest.joinToString { it.truncatedHash() }}")
        api.fetchAccountGatewayDetails(
            accountsToRequest = accountsToRequest,
            onStateVersion = onStateVersion
        ).onSuccess { result ->
            val receivedAccountAddresses = result.map { it.first.address }
            logger.d("☁️ <= ${receivedAccountAddresses.joinToString { it.truncatedHash() }}")
            accountsRequested.update { value -> value subtract receivedAccountAddresses.toSet() }

            if (result.isNotEmpty()) {
                withContext(dispatcher) {
                    logger.d("\uD83D\uDCBD Inserting accounts ${result.map { it.first.address.truncatedHash() }}")
                    dao.updateAccountData(result)
                }
            }
        }.onFailure {
            accountsRequested.update { value -> value subtract accountsToRequest }
            throw it
        }
    }

    private fun Flow<List<AccountPortfolioResponse>>.collectCachedData() = map { portfolio ->
        val result = mutableMapOf<String, AccountCachedData>()
        val cacheMinBoundary = Instant.ofEpochMilli(accountCacheValidity())
        val addressesOnNetwork = getProfileUseCase.accountsOnCurrentNetwork().map { it.address }
        portfolio.forEach { cache ->
            if (cache.accountSynced == null || cache.accountSynced < cacheMinBoundary || cache.address !in addressesOnNetwork) {
                return@forEach
            }

            // Parse details for this account
            val cachedDetails = AccountCachedData(
                stateVersion = cache.stateVersion,
                accountType = cache.accountType,
                firstTransactionDate = cache.firstTransactionDate
            )

            // Compile all resources owned by this account
            if (cache.stateVersion != null && cache.resource != null && cache.amount != null) {
                when (val resource = cache.resource.toResource(cache.amount)) {
                    is Resource.FungibleResource -> {
                        result[cache.address] = result.getOrDefault(cache.address, cachedDetails).also {
                            it.fungibles.add(resource)
                        }
                    }

                    is Resource.NonFungibleResource -> {
                        result[cache.address] = result.getOrDefault(cache.address, cachedDetails).also {
                            it.nonFungibles.add(resource)
                        }
                    }
                }
            } else {
                result[cache.address] = cachedDetails
            }
        }
        result
    }

    private fun Flow<MutableMap<String, AccountCachedData>>.compileAccountAddressAssets(): Flow<List<AccountAddressWithAssets>> =
        transform { cached ->
            val stateVersion = cached.values.mapNotNull { it.stateVersion }.maxOrNull() ?: run {
                emit(emptyList())
                return@transform
            }

            val allValidatorAddresses = cached.map { it.value.validatorAddresses() }.flatten().toSet()
            val cachedValidators = dao.getCachedValidators(allValidatorAddresses, stateVersion).toMutableMap()
            val newValidators = runCatching {
                api.fetchValidators(
                    allValidatorAddresses - cachedValidators.keys,
                    stateVersion
                ).validators.asValidators().onEach {
                    cachedValidators[it.address] = it
                }
            }.onFailure { error ->
                cacheErrors.value = error
            }.getOrNull() ?: return@transform

            if (newValidators.isNotEmpty()) {
                logger.d("\uD83D\uDCBD Inserting validators")
                dao.insertValidators(newValidators.asValidatorEntities(SyncInfo(InstantGenerator(), stateVersion)))
            }

            val allPoolAddresses = cached.map { it.value.poolAddresses() }.flatten().toSet()
            val cachedPools = dao.getCachedPools(allPoolAddresses, stateVersion).toMutableMap()
            val unknownPools = allPoolAddresses - cachedPools.keys
            if (unknownPools.isNotEmpty()) {
                logger.d("\uD83D\uDCBD Inserting pools")

                val newPools = runCatching {
                    api.fetchPools(unknownPools, stateVersion)
                }.onFailure { error ->
                    cacheErrors.value = error
                }.getOrNull() ?: return@transform

                if (newPools.poolItems.isNotEmpty()) {
                    val join = newPools.poolItems.asPoolsResourcesJoin(SyncInfo(InstantGenerator(), stateVersion))
                    dao.updatePools(pools = join)
                } else {
                    emit(
                        cached.mapNotNull {
                            it.value.toAccountAddressWithAssets(
                                accountAddress = it.key,
                                pools = cachedPools,
                                validators = cachedValidators
                            )
                        }
                    )
                }
            } else {
                emit(
                    cached.mapNotNull {
                        it.value.toAccountAddressWithAssets(
                            accountAddress = it.key,
                            pools = cachedPools,
                            validators = cachedValidators
                        )
                    }
                )
            }
        }

    private data class AccountCachedData(
        val stateVersion: Long?,
        val accountType: AccountType?,
        val firstTransactionDate: Instant?,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
    ) {

        fun poolAddresses() = fungibles.mapNotNull { it.poolAddress }.toSet()

        fun validatorAddresses(): Set<String> = fungibles.mapNotNull {
            it.validatorAddress
        }.toSet() + nonFungibles.mapNotNull {
            it.validatorAddress
        }

        @Suppress("LongMethod")
        fun toAccountAddressWithAssets(
            accountAddress: String,
            pools: Map<String, Pool>,
            validators: Map<String, ValidatorDetail>
        ): AccountAddressWithAssets? {
            if (stateVersion == null) return null

            val stakeUnitAddressToValidator = validators.mapKeys { it.value.stakeUnitResourceAddress }
            val claimTokenAddressToValidator = validators.mapKeys { it.value.claimTokenResourceAddress }

            val resultingPoolUnits = mutableListOf<PoolUnit>()
            val resultingLSUs = mutableListOf<LiquidStakeUnit>()
            val resultingStakeClaims = mutableListOf<StakeClaim>()

            val resultingFungibles = fungibles.toMutableList()
            val resultingNonFungibles = nonFungibles.toMutableList()

            val fungiblesIterator = resultingFungibles.iterator()
            while (fungiblesIterator.hasNext()) {
                val fungible = fungiblesIterator.next()

                val pool = pools[fungible.poolAddress]?.takeIf { pool ->
                    // The fungible claims that it is part of the poolAddress.
                    // We need to check if pool points back to this resource
                    pool.metadata.poolUnit() == fungible.resourceAddress
                }
                if (pool != null) {
                    resultingPoolUnits.add(
                        PoolUnit(
                            stake = fungible,
                            pool = pool,
                        )
                    )

                    fungiblesIterator.remove()
                }

                val validatorDetails = stakeUnitAddressToValidator[fungible.resourceAddress]?.takeIf { validator ->
                    // The fungible claims that it is a LSU,
                    // so we need to check if the validator points back to this resource
                    validator.address == fungible.validatorAddress
                }
                if (validatorDetails != null) {
                    resultingLSUs.add(LiquidStakeUnit(fungible, validatorDetails))

                    // Remove this fungible from the list as it will be included as an lsu
                    fungiblesIterator.remove()
                }
            }

            val nonFungiblesIterator = resultingNonFungibles.iterator()
            while (nonFungiblesIterator.hasNext()) {
                val nonFungible = nonFungiblesIterator.next()

                val validatorDetails = claimTokenAddressToValidator[nonFungible.resourceAddress]?.takeIf { validator ->
                    // The non-fungible claims that it is a claim token,
                    // so we need to check if the validator points back to this resource
                    validator.address == nonFungible.validatorAddress
                }
                if (validatorDetails != null) {
                    resultingStakeClaims.add(StakeClaim(nonFungible, validatorDetails))

                    // Remove this non-fungible from the list as it will be included as a stake claim
                    nonFungiblesIterator.remove()
                }
            }

            return AccountAddressWithAssets(
                address = accountAddress,
                details = AccountDetails(
                    stateVersion = stateVersion,
                    accountType = accountType,
                    firstTransactionDate = firstTransactionDate
                ),
                assets = Assets(
                    tokens = resultingFungibles.sorted().map { Token(it) },
                    nonFungibles = resultingNonFungibles.sorted().map { NonFungibleCollection(it) },
                    poolUnits = resultingPoolUnits,
                    liquidStakeUnits = resultingLSUs,
                    stakeClaims = resultingStakeClaims,
                )
            )
        }
    }

    private data class AccountAddressWithAssets(
        val address: String,
        val details: AccountDetails?,
        val assets: Assets?
    ) {

        fun toAccountWithAssets(account: Network.Account, dao: StateDao) = AccountWithAssets(
            account = account,
            details = details,
            assets = details?.stateVersion?.let { stateVersion ->
                val nonFungibles = assets?.nonFungibles?.map { nonFungible ->
                    val items = dao.getOwnedNfts(account.address, nonFungible.collection.resourceAddress, stateVersion)
                        .map { it.toItem() }.sorted()
                    nonFungible.copy(collection = nonFungible.collection.copy(items = items))
                }.orEmpty()

                val updatedClaims = assets?.stakeClaims?.map { stakeClaim ->
                    val items = dao.getOwnedNfts(account.address, stakeClaim.resourceAddress, stateVersion)
                        .map { it.toItem() }
                        .sorted()
                    stakeClaim.copy(nonFungibleResource = stakeClaim.nonFungibleResource.copy(items = items))
                }.orEmpty()

                assets?.copy(nonFungibles = nonFungibles, stakeClaims = updatedClaims)
            } ?: assets
        )
    }

    companion object {
        private val logger = Timber.tag("AccountsStateCache")
    }
}
