package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asAccountResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.PoolResourceJoin.Companion.asPoolResourceJoin
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.StateVersionEntity
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import javax.inject.Inject

interface StateRepository {

    fun observeAccountsResources(accounts: List<Network.Account>): Flow<Map<Network.Account, AccountOnLedger>>
}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao
) : StateRepository {

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    override fun observeAccountsResources(
        accounts: List<Network.Account>
    ): Flow<Map<Network.Account, AccountOnLedger>> = cacheDelegate
        .observeAllResources(accounts)
        .transform { cachedAccounts ->
            emit(cachedAccounts.mapValues { it.value.toAccountDetails() })

            val remainingAccounts = accounts.toSet() - cachedAccounts.keys
            if (remainingAccounts.isNotEmpty()) {
                Timber.tag("Bakos").d("Fetching for account ${remainingAccounts.first().displayName}")
                stateApiDelegate.fetchAllResources(
                    accounts = setOf(remainingAccounts.first()),
                    onStateVersion = { stateVersion ->
                        stateDao.insertStateVersion(StateVersionEntity(stateVersion))
                    },
                    onAccount = { account, gatewayDetails ->
                        val accountMetadataItems = gatewayDetails.accountMetadata?.asMetadataItems()?.toMutableList()
                        val syncInfo = SyncInfo(synced = InstantGenerator(), stateVersion = gatewayDetails.ledgerState.stateVersion)
                        val fungibleEntityPairs = gatewayDetails.fungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }
                        val nonFungibleEntityPairs = gatewayDetails.nonFungibles.map { item ->
                            item.asAccountResourceJoin(account.address, syncInfo)
                        }

                        stateDao.updateAccountData(
                            accountAddress = account.address,
                            accountTypeMetadataItem = accountMetadataItems?.consume(),
                            syncInfo = syncInfo,
                            accountWithResources = fungibleEntityPairs + nonFungibleEntityPairs
                        )

                        val poolAddresses = fungibleEntityPairs.mapNotNull { it.second.poolAddress }.toSet()
                        val pools = stateApiDelegate.getPoolDetails(poolAddresses = poolAddresses, stateVersion = syncInfo.stateVersion)
                        cacheDelegate.storePoolDetails(pools, syncInfo)

                        val validatorAddresses = (fungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        } + nonFungibleEntityPairs.mapNotNull {
                            it.second.validatorAddress
                        }).toSet()
                        val validators = stateApiDelegate.getValidatorsDetails(
                            validatorsAddresses = validatorAddresses,
                            stateVersion = syncInfo.stateVersion
                        )
                        cacheDelegate.storeValidatorDetails(validators, syncInfo)
                    }
                )
            }
        }.distinctUntilChanged()
}
