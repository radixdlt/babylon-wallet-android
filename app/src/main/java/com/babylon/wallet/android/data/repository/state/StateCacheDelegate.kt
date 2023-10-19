package com.babylon.wallet.android.data.repository.state

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.repository.cache.OwnedFungibleEntity.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.OwnedNonFungibleEntity.Companion.asEntityPair
import com.babylon.wallet.android.data.repository.cache.StateDao
import com.babylon.wallet.android.data.repository.cache.SyncInfo
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.Resources
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import kotlin.time.measureTime

class StateCacheDelegate(
    private val stateDao: StateDao
) {

    fun getAllResources(accounts: List<Network.Account>): Map<Network.Account, Resources> {
        val cachedResults = mutableMapOf<
                Network.Account,
                Pair<MutableList<Resource.FungibleResource>, MutableList<Resource.NonFungibleResource>>
        >()
        val accountAddresses = accounts.associateBy { it.address }

        Timber.tag("Bakos").d("DB Fetching")
        val time = measureTime {
            val allAddresses = accountAddresses.keys.toList()
            stateDao.getAccountFungibles(allAddresses).forEach { entry ->
                val account = accountAddresses[entry.key.accountAddress] ?: return@forEach
                val resources = cachedResults.getOrPut(account) { mutableListOf<Resource.FungibleResource>() to mutableListOf() }
                resources.first.add(entry.value.toResource(withOwnedAmount = entry.key.amount))
            }
            stateDao.getAccountNonFungibles(allAddresses).forEach { entry ->
                val account = accountAddresses[entry.key.accountAddress] ?: return@forEach
                val resources = cachedResults.getOrPut(account) { mutableListOf<Resource.FungibleResource>() to mutableListOf() }
                resources.second.add(entry.value.toResource(withOwnedAmount = entry.key.amount))
            }
        }

        Timber.tag("Bakos").d("DB: $time")
        return cachedResults.mapValues {
            Resources(fungibles = it.value.first, nonFungibles = it.value.second)
        }
    }

    fun insertResources(
        accountAddress: String,
        ledgerState: LedgerState,
        fungibles: List<FungibleResourcesCollectionItem>,
        nonFungibles: List<NonFungibleResourcesCollectionItem>
    ) {
        val syncInfo = SyncInfo(synced = InstantGenerator(), epoch = ledgerState.epoch)
        with(fungibles.map { item -> item.asEntityPair(accountAddress = accountAddress, syncInfo = syncInfo) }) {
            stateDao.insertFungibles(map { it.second })
            stateDao.insertOwnedFungibles(map { it.first })
        }
        with(nonFungibles.map { item -> item.asEntityPair(accountAddress = accountAddress, syncInfo = syncInfo) }) {
            stateDao.insertNonFungibles(map { it.second })
            stateDao.insertOwnedNonFungibles(map { it.first })
        }
    }

}
