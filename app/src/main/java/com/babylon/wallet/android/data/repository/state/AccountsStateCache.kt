package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.fetchAccountGatewayDetails
import com.babylon.wallet.android.data.gateway.extensions.fetchFungibleAmountPerAccount
import com.babylon.wallet.android.data.gateway.extensions.fetchPools
import com.babylon.wallet.android.data.gateway.extensions.fetchValidators
import com.babylon.wallet.android.data.gateway.extensions.fetchVaultDetails
import com.babylon.wallet.android.data.repository.cache.database.AccountPortfolioResponse
import com.babylon.wallet.android.data.repository.cache.database.PoolEntity.Companion.asPoolsResourcesJoin
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateDao.Companion.accountCacheValidity
import com.babylon.wallet.android.data.repository.cache.database.StateDatabase
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.data.repository.cache.database.ValidatorEntity.Companion.asValidatorEntity
import com.babylon.wallet.android.data.repository.cache.database.getCachedPools
import com.babylon.wallet.android.data.repository.cache.database.getCachedValidators
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.utils.truncatedHash
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.AccountDetails
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.AccountType
import rdx.works.core.domain.resources.metadata.poolUnit
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.hiddenFungibles
import rdx.works.core.sargon.hiddenNonFungibles
import rdx.works.core.sargon.hiddenPools
import rdx.works.core.toUnitResult
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountsStateCache @Inject constructor(
    private val api: StateApi,
    private val dao: StateDao,
    private val database: StateDatabase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val accountsMemoryCache = MutableStateFlow<List<AccountAddressWithAssets>?>(null)
    private val cacheErrors = MutableStateFlow<Throwable?>(null)
    private val accountsRequested = MutableStateFlow<Set<AccountAddress>>(emptySet())
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
        accounts: List<Account>,
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
            val hiddenNftIds = profileRepository.profile.first().appPreferences.assets.hiddenNonFungibles()
            val accountsToReturn = accounts.map { account ->
                cachedAccounts.find { it.address == account.address }?.toAccountWithAssets(
                    account = account,
                    dao = dao
                )?.let { accountWithAssets ->
                    accountWithAssets.copy(
                        assets = accountWithAssets.assets?.copy(
                            nonFungibles = accountWithAssets.assets.nonFungibles.mapNotNull { nonFungible ->
                                if (nonFungible.collection.items.isNotEmpty()) {
                                    val newItems = nonFungible.collection.items.filterNot { it.globalId in hiddenNftIds }
                                    nonFungible.copy(
                                        collection = nonFungible.collection.copy(
                                            items = newItems,
                                            displayAmount = newItems.size.toLong()
                                        )
                                    ).takeIf { newItems.isNotEmpty() }
                                } else {
                                    nonFungible
                                }
                            }
                        )
                    )
                } ?: AccountWithAssets(account = account)
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

    /**
     * First checks if we have a vault address cached for XRD. If so we can immediately request [fetchVaultDetails] in order to query
     * the XRD amount at that vault directly. If no vault information is found, we need to make the more expensive request
     * of fetching account entity details. For more info about this method check [fetchFungibleAmountPerAccount].
     */
    suspend fun getOwnedXRD(accounts: List<Account>): Result<Map<Account, Decimal192>> = withContext(dispatcher) {
        runCatching {
            if (accounts.isEmpty()) return@withContext Result.success(emptyMap())

            val xrdAddress = XrdResource.address(networkId = accounts.first().networkId)
            // Initialize requesting accounts with 0 amounts
            val result = accounts.associateWith { 0.toDecimal192() }.toMutableMap()

            val accountsWithMaybeXRDVaults: Map<Account, VaultAddress?> = accounts.associateWith { account ->
                dao.getAccountResourceJoin(accountAddress = account.address, resourceAddress = xrdAddress)?.vaultAddress
            }

            // For accounts that we know the vault address were the XRD is stored, we can query directly
            val vaultsPerAccount = accountsWithMaybeXRDVaults.filter { it.value != null }.map {
                requireNotNull(it.value) to it.key
            }.toMap()
            api.fetchVaultDetails(vaultsPerAccount.keys).forEach { entry ->
                val xrdAmount = entry.value
                val account = vaultsPerAccount[entry.key]

                if (account != null) {
                    result[account] = xrdAmount
                }
            }

            // For accounts with no vault information, we need to find the resource while requesting entity details for such accounts
            val accountsWithNoVaults = accountsWithMaybeXRDVaults.filter { it.value == null }.map { it.key }.toSet()
            if (accountsWithNoVaults.isNotEmpty()) {
                api.fetchFungibleAmountPerAccount(
                    accounts = accountsWithNoVaults.map { it.address }.toSet(),
                    resourceAddress = xrdAddress,
                    onStateVersion = null
                ).getOrNull()?.forEach { entry ->
                    val account = accountsWithNoVaults.find { it.address == entry.key }

                    if (account != null) {
                        result[account] = entry.value
                    }
                }
            }

            result
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
        accountAddresses: Set<AccountAddress>,
        onStateVersion: Long? = null,
    ) {
        val addressesInProgress = accountsRequested.getAndUpdate { value -> value union accountAddresses }
        val accountsToRequest = accountAddresses subtract addressesInProgress
        if (accountsToRequest.isEmpty()) return

        logger.d("☁️ ${accountsToRequest.joinToString { it.formatted() }}")
        api.fetchAccountGatewayDetails(
            accountsToRequest = accountsToRequest,
            onStateVersion = onStateVersion
        ).onSuccess { result ->
            val receivedAccountAddresses = result.map { AccountAddress.init(it.first.address) }
            logger.d("☁️ <= ${receivedAccountAddresses.joinToString { it.formatted() }}")
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
        val result = mutableMapOf<AccountAddress, AccountCachedData>()
        val cacheMinBoundary = Instant.ofEpochMilli(accountCacheValidity())
        val addressesOnNetwork = profileRepository.profile.first().activeAccountsOnCurrentNetwork.map { it.address }
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

    private fun Flow<MutableMap<AccountAddress, AccountCachedData>>.compileAccountAddressAssets() = filterHiddenAssets()
        .transform { cached ->
            val stateVersion = cached.values.mapNotNull { it.stateVersion }.maxOrNull() ?: run {
                emit(emptyList())
                return@transform
            }

            val allValidatorAddresses = cached.map { it.value.validatorAddresses() }.flatten().toSet()
            val cachedValidators = dao.getCachedValidators(allValidatorAddresses, stateVersion).toMutableMap()
            val newValidators = runCatching {
                val validatorItems = api.fetchValidators(
                    allValidatorAddresses - cachedValidators.keys,
                    stateVersion
                ).validators

                val syncInfo = SyncInfo(InstantGenerator(), stateVersion)
                validatorItems.map { it.asValidatorEntity(syncInfo) }
                    .onEach { entity ->
                        cachedValidators[entity.address] = entity.asValidatorDetail()
                    }
            }.onFailure { cacheErrors.value = it }.getOrNull() ?: return@transform

            if (newValidators.isNotEmpty()) {
                logger.d("\uD83D\uDCBD Inserting validators")
                dao.insertValidators(newValidators)
            }

            val allPoolAddresses = cached.map { it.value.poolAddresses() }.flatten().toSet()
            val cachedPools = dao.getCachedPools(allPoolAddresses, stateVersion).toMutableMap()
            val unknownPools = allPoolAddresses - cachedPools.keys
            if (unknownPools.isNotEmpty()) {
                logger.d("\uD83D\uDCBD Inserting pools")
                val newPools = runCatching {
                    api.fetchPools(unknownPools.toSet(), stateVersion)
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

    private fun Flow<MutableMap<AccountAddress, AccountCachedData>>.filterHiddenAssets() = map { cached ->
        val assetPreferences = profileRepository.profile.first().appPreferences.assets
        val hiddenPoolAddresses = assetPreferences.hiddenPools().toSet()
        val hiddenFungibleAddresses = assetPreferences.hiddenFungibles().toSet()

        cached.mapValues { entry ->
            entry.value.copy(
                fungibles = entry.value.fungibles.filterNot { it.address in hiddenFungibleAddresses }
                    .filterNot { it.poolAddress in hiddenPoolAddresses }
                    .toMutableList()
            )
        }.toMutableMap()
    }

    private data class AccountCachedData(
        val stateVersion: Long?,
        val accountType: AccountType?,
        val firstTransactionDate: Instant?,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf(),
    ) {

        fun poolAddresses() = fungibles.mapNotNull { it.poolAddress }.toSet()

        fun validatorAddresses(): Set<ValidatorAddress> = fungibles.mapNotNull {
            it.validatorAddress
        }.toSet() + nonFungibles.mapNotNull {
            it.validatorAddress
        }

        @Suppress("LongMethod")
        fun toAccountAddressWithAssets(
            accountAddress: AccountAddress,
            pools: Map<PoolAddress, Pool>,
            validators: Map<ValidatorAddress, Validator>
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
                    pool.metadata.poolUnit() == fungible.address
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

                val validatorDetails = stakeUnitAddressToValidator[fungible.address]?.takeIf { validator ->
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

                val validatorDetails = claimTokenAddressToValidator[
                    nonFungible.address
                ]?.takeIf { validator ->
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
        val address: AccountAddress,
        val details: AccountDetails?,
        val assets: Assets?
    ) {

        fun toAccountWithAssets(account: Account, dao: StateDao): AccountWithAssets {
            return AccountWithAssets(
                account = account,
                details = details,
                assets = assets?.copy(
                    nonFungibles = assets.nonFungibles.map { nonFungible ->
                        val items = dao.getOwnedNfts(account.address, nonFungible.collection.address)
                            .map { it.toItem() }.sorted()
                        nonFungible.copy(collection = nonFungible.collection.copy(items = items))
                    },
                    stakeClaims = assets.stakeClaims.map { stakeClaim ->
                        val items = dao.getOwnedNfts(account.address, stakeClaim.resourceAddress)
                            .map { it.toItem() }
                            .sorted()
                        stakeClaim.copy(nonFungibleResource = stakeClaim.nonFungibleResource.copy(items = items))
                    }
                )
            )
        }
    }

    companion object {
        private val logger = Timber.tag("AccountsStateCache")
    }
}
