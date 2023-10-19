package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.repository.cache.StateDao
import com.babylon.wallet.android.domain.model.resources.Resources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

interface StateRepository {

    fun getAccountsState(accounts: List<Network.Account>): Flow<Map<Network.Account, Resources>>

}

class StateRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val stateDao: StateDao
) : StateRepository {

    private val cacheDelegate = StateCacheDelegate(stateDao = stateDao)
    private val stateApiDelegate = StateApiDelegate(stateApi = stateApi)

    override fun getAccountsState(
        accounts: List<Network.Account>
    ): Flow<Map<Network.Account, Resources>> = flow {

        val cachedResources = cacheDelegate.getAllResources(accounts)

        if (cachedResources.isNotEmpty()) {
            emit(cachedResources)
        }

        val remainingAccounts = accounts.toSet() - cachedResources.keys
        if (remainingAccounts.isEmpty()) return@flow

        stateApiDelegate.fetchAllResources(
            accounts = remainingAccounts
        ) { account, ledgerState, fungibles, nonFungibles ->
            cacheDelegate.insertResources(
                accountAddress = account.address,
                ledgerState = ledgerState,
                fungibles = fungibles,
                nonFungibles = nonFungibles
            )
        }
    }

}
