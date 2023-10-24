package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.database.AccountResourceJoin.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.cache.database.SyncInfo
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.AccountOnLedger
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, CachedDetails>> {
        val accountAddresses = accounts.map { it.address }
        return stateDao.observeAccountsPortfolio(accountAddresses).transform { detailsWithResources ->
            val result = mutableMapOf<Network.Account, CachedDetails>()

            detailsWithResources.forEach { accountDetailsAndResources ->
                val account = accounts.find { it.address == accountDetailsAndResources.address } ?: return@forEach

                // Parse details for this account
                val cachedDetails = CachedDetails(
                    stateVersion = accountDetailsAndResources.accountStateVersion,
                    accountDetails = AccountDetails(
                        typeMetadataItem = accountDetailsAndResources.accountType?.let { AccountTypeMetadataItem(it) }
                    )
                )

                // Compile all resources owned by this account
                if (accountDetailsAndResources.resource != null && accountDetailsAndResources.amount != null) {
                    when (val resource = accountDetailsAndResources.resource.toResource(accountDetailsAndResources.amount)) {
                        is Resource.FungibleResource -> {
                            result[account] = result.getOrDefault(account, cachedDetails).also {
                                it.fungibles.add(resource)
                            }
                        }

                        is Resource.NonFungibleResource -> {
                            result[account] = result.getOrDefault(account, cachedDetails).also {
                                it.nonFungibles.add(resource)
                            }
                        }
                    }
                } else {
                    result[account] = cachedDetails
                }
            }

            emit(result)
        }.distinctUntilChanged()
    }

    fun insertAccountDetails(
        accountAddress: String,
        gatewayDetails: StateApiDelegate.AccountGatewayDetails
    ) {
        val accountMetadataItems = gatewayDetails.accountMetadata?.asMetadataItems()?.toMutableList()
        val syncInfo = SyncInfo(synced = InstantGenerator(), stateVersion = gatewayDetails.ledgerState.stateVersion)
        val accountWithResources = gatewayDetails.fungibles.map { item ->
            item.asEntityPair(accountAddress, syncInfo)
        } + gatewayDetails.nonFungibles.map { item ->
            item.asEntityPair(accountAddress, syncInfo)
        }

        stateDao.updateAccountData(
            accountAddress = accountAddress,
            accountTypeMetadataItem = accountMetadataItems?.consume(),
            syncInfo = syncInfo,
            accountWithResources = accountWithResources
        )
    }

    data class CachedDetails(
        val stateVersion: Long,
        val accountDetails: AccountDetails,
        val fungibles: MutableList<Resource.FungibleResource> = mutableListOf(),
        val nonFungibles: MutableList<Resource.NonFungibleResource> = mutableListOf()
    ) {

        fun toAccountDetails() = AccountOnLedger(
            accountDetails,
            fungibles,
            nonFungibles,
            emptyList(),
            mapOf()
        )

    }
}
