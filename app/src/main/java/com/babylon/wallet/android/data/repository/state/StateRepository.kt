package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.repository.cache.StateDao
import com.babylon.wallet.android.domain.model.resources.Resources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import javax.inject.Inject

interface StateRepository {

    fun observeAccountsResources(accounts: List<Network.Account>): Flow<Map<Network.Account, Resources>>

}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao
) : StateRepository {

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    override fun observeAccountsResources(
        accounts: List<Network.Account>
    ): Flow<Map<Network.Account, Resources>> = cacheDelegate
        .observeAllResources(accounts)
        .onEach { cachedAccountsWithResources ->
            Timber.tag("Bakos").d("Found data for accounts: ${cachedAccountsWithResources.keys.map { it.displayName }}")
            val remainingAccounts = accounts.toSet() - cachedAccountsWithResources.keys

            if (remainingAccounts.isEmpty()) return@onEach
            Timber.tag("Bakos").d("Fetching for account ${remainingAccounts.first().displayName}")
            stateApiDelegate.fetchAllResources(
                accounts = listOf(remainingAccounts.first())
            ) { account, ledgerState, fungibles, nonFungibles ->
                Timber.tag("Bakos").d("API received for account ${account.displayName}")
                cacheDelegate.insertResources(
                    accountAddress = account.address,
                    ledgerState = ledgerState,
                    fungibles = fungibles,
                    nonFungibles = nonFungibles
                )
            }
        }

}
