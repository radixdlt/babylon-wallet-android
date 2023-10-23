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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, Pair<AccountDetails, Resources>>> {
        val accountAddresses = accounts.map { it.address }
        return stateDao.observeAccountsPortfolio(accountAddresses).transform { detailsWithResources ->
            val result = mutableMapOf<Network.Account, MutableDetails>()

            detailsWithResources.forEach { accountDetailsAndResources ->
                val account = accounts.find { it.address == accountDetailsAndResources.address } ?: return@forEach

                // Parse details for this account
                val accountDetails = AccountDetails(
                    typeMetadataItem = accountDetailsAndResources.accountType?.let { AccountTypeMetadataItem(it) }
                )

                // Compile all resources owned by this account
                if (accountDetailsAndResources.resource != null && accountDetailsAndResources.amount != null) {
                    when (val resource = accountDetailsAndResources.resource.toResource(accountDetailsAndResources.amount)) {
                        is Resource.FungibleResource -> {
                            result[account] = result.getOrDefault(account, MutableDetails(accountDetails = accountDetails)).also {
                                it.fungibles.add(resource)
                            }
                        }

                        is Resource.NonFungibleResource -> {
                            result[account] = result.getOrDefault(account, MutableDetails(accountDetails = accountDetails)).also {
                                it.nonFungibles.add(resource)
                            }
                        }
                    }
                } else {
                    result[account] = MutableDetails(accountDetails = accountDetails)
                }
            }

            emit(
                result.mapValues { entry ->
                    val accountDetails = entry.value.accountDetails
                    accountDetails to Resources(
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
        fungibles: List<FungibleResourcesCollectionItem>,
        nonFungibles: List<NonFungibleResourcesCollectionItem>,
        syncInfo: SyncInfo
    ) {
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

    private data class MutableDetails(
        val accountDetails: AccountDetails,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf()
    )
}
