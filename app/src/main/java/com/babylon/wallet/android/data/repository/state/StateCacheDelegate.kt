package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.database.AccountDetailsEntity
import com.babylon.wallet.android.data.repository.cache.database.AccountResourcesPortfolio.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.Resources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, Resources>> {
        val accountAddresses = accounts.map { it.address }
        return combine(
            stateDao.observeAccountDetails(accountAddresses),
            stateDao.observeAccountsPortfolio(accountAddresses)
        ) { accountsDetails, accountsWithResources ->
            accountsDetails to accountsWithResources
        }.map { pair ->
            val accountsDetails = pair.first
            val accountsWithResources = pair.second

            val result = mutableMapOf<Network.Account, MutableResources>()
            // First collect resources for all accounts
            accountsWithResources.forEach { accountWithResource ->
                val account = accounts.find { it.address == accountWithResource.address } ?: return@forEach
                when (val resource = accountWithResource.resource.toResource(accountWithResource.amount)) {
                    is Resource.FungibleResource -> {
                        result[account] = result.getOrDefault(account, MutableResources()).also {
                            it.fungibles.add(resource)
                        }
                    }
                    is Resource.NonFungibleResource -> {
                        result[account] = result.getOrDefault(account, MutableResources()).also {
                            it.nonFungibles.add(resource)
                        }
                    }
                }
            }

            // Then add accounts which may have no resources but appear in account details
            accountsDetails.forEach { accountDetails ->
                val account = accounts.find { it.address == accountDetails.address } ?: return@forEach
                if (!result.contains(account)) {
                    result[account] = MutableResources()
                }
            }


            result.mapValues {
                Resources(
                    fungibles = it.value.fungibles,
                    nonFungibles = it.value.nonFungibles
                )
            }
        }.distinctUntilChanged()
    }

    fun insertAccountDetails(
        accountAddress: String,
        accountType: AccountTypeMetadataItem?,
        ledgerState: LedgerState,
        fungibles: List<FungibleResourcesCollectionItem>,
        nonFungibles: List<NonFungibleResourcesCollectionItem>
    ) {
        val syncInfo = SyncInfo(synced = InstantGenerator(), epoch = ledgerState.epoch)

        stateDao.updateAccountData(
            accountDetails = AccountDetailsEntity(
                address = accountAddress,
                accountType = accountType?.type,
                synced = syncInfo.synced,
                epoch = syncInfo.epoch
            ),
            accountWithResources = fungibles.map { item ->
                item.asEntityPair(accountAddress, syncInfo)
            } + nonFungibles.map { item ->
                item.asEntityPair(accountAddress, syncInfo)
            }
        )
    }

    private data class MutableResources(
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf()
    )
}
