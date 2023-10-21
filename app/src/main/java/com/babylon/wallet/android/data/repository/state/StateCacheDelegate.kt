package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.Resources
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, Pair<AccountDetails, Resources>>> {
        val accountAddresses = accounts.map { it.address }
        return combine(
            stateDao.observeAccountsPortfolio(accountAddresses),
            stateDao.observeAccountDetails(accountAddresses).distinctUntilChanged()
        ) { accountsWithResources, accountsDetails ->
            accountsWithResources to accountsDetails
        }.transform { detailsWithResources ->
            val accountsWithResources = detailsWithResources.first
            val accountDetails = detailsWithResources.second

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

            // Every account that has no resources in AccountResourcesPortfolio, may mean that either it owns no accounts
            // or we have not received any information from Gateway yet regarding this account.

            // So we check if we have received any account details. If so that means that accounts that don't exist in
            // accountsWithResources map but exist in details, mean that they own no accounts so we should update the client
            // to know definitely that this account is not yet pending to receive all the data.
            accountDetails.forEach { details ->
                val account = accounts.find { it.address == details.address } ?: return@forEach

                if (!result.contains(account)) {
                    result[account] = MutableResources()
                }
            }

            emit(
                result.mapValues { entry ->
                    val account = entry.key
                    val details = accountDetails.find { it.address == account.address }?.toAccountDetails() ?: AccountDetails()
                    details to Resources(
                        fungibles = entry.value.fungibles,
                        nonFungibles = entry.value.nonFungibles
                    )
                }
            )
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
            accountAddress = accountAddress,
            accountTypeMetadataItem = accountType,
            syncInfo = syncInfo,
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
