package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.FungibleResourceEntity
import com.babylon.wallet.android.data.repository.cache.NonFungibleResourceEntity
import com.babylon.wallet.android.data.repository.cache.OwnedFungibleEntity
import com.babylon.wallet.android.data.repository.cache.OwnedFungibleEntity.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.OwnedNonFungibleEntity
import com.babylon.wallet.android.data.repository.cache.OwnedNonFungibleEntity.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.StateDao
import com.babylon.wallet.android.data.repository.cache.SyncInfo
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.Resources
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun observeAllResources(accounts: List<Network.Account>): Flow<Map<Network.Account, Resources>> {
        val accountAddresses = accounts.associateBy { it.address }
        val allAddresses = accountAddresses.keys.toList()
        return combine(
            stateDao.observeAccountFungibles(allAddresses),
            stateDao.observeAccountNonFungibles(allAddresses)
        ) { ownedFungiblesPerAccount: Map<OwnedFungibleEntity, FungibleResourceEntity>,
            ownedNonFungiblesPerAccount: Map<OwnedNonFungibleEntity, NonFungibleResourceEntity> ->

            val cachedResults = mutableMapOf<
                    Network.Account,
                    Pair<MutableList<Resource.FungibleResource>, MutableList<Resource.NonFungibleResource>>
            >()

            ownedFungiblesPerAccount.forEach { entry ->
                val account = accountAddresses[entry.key.accountAddress] ?: return@forEach
                val resources = cachedResults.getOrPut(account) { mutableListOf<Resource.FungibleResource>() to mutableListOf() }
                resources.first.add(entry.value.toResource(withOwnedAmount = entry.key.amount))
            }

            ownedNonFungiblesPerAccount.forEach { entry ->
                val account = accountAddresses[entry.key.accountAddress] ?: return@forEach
                val resources = cachedResults.getOrPut(account) { mutableListOf<Resource.FungibleResource>() to mutableListOf() }
                resources.second.add(entry.value.toResource(withOwnedAmount = entry.key.amount))
            }

            cachedResults.mapValues {
                Resources(fungibles = it.value.first, nonFungibles = it.value.second)
            }
        }
    }

    fun insertResources(
        accountAddress: String,
        ledgerState: LedgerState,
        fungibles: List<FungibleResourcesCollectionItem>,
        nonFungibles: List<NonFungibleResourcesCollectionItem>
    ) {
        val syncInfo = SyncInfo(synced = InstantGenerator(), epoch = ledgerState.epoch)
        stateDao.updateOwnedResources(
            fungibles = fungibles.map { item -> item.asEntityPair(accountAddress = accountAddress, syncInfo = syncInfo) },
            nonFungibles = nonFungibles.map { item -> item.asEntityPair(accountAddress = accountAddress, syncInfo = syncInfo) }
        )
    }

}
