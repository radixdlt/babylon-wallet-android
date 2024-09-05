package com.babylon.wallet.android.data.repository.locker

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.paginateAccountLockerVaultItems
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerAddress
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.StateAccountLockersTouchedAtRequest
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerDao
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerTouchedAtEntity
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerVaultItemEntity
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AccountLockerRepository {

    suspend fun getAvailableAccountLockerDeposits(
        accountAddress: AccountAddress,
        lockerAddresses: Set<LockerAddress>
    ): Result<Set<LockerAddress>>
}

class AccountLockerRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val accountLockerDao: AccountLockerDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : AccountLockerRepository {

    override suspend fun getAvailableAccountLockerDeposits(
        accountAddress: AccountAddress,
        lockerAddresses: Set<LockerAddress>
    ): Result<Set<LockerAddress>> {
        return withContext(dispatcher) {
            runCatching {
                val statesRequest = StateAccountLockersTouchedAtRequest(
                    accountLockers = lockerAddresses.map {
                        AccountLockerAddress(
                            lockerAddress = it.string,
                            accountAddress = accountAddress.string
                        )
                    }
                )
                val refreshedStates = stateApi.accountLockersTouchedAt(statesRequest)
                    .toResult()
                    .getOrThrow()
                    .let { response -> AccountLockerTouchedAtEntity.from(response) }
                val cachedStates = accountLockerDao.getTouchedAt(
                    accountAddress = accountAddress,
                    lockerAddresses = lockerAddresses
                )

                val claimableLockerAddresses = refreshedStates.mapNotNull { refreshedState ->
                    val cachedState = cachedStates.find { it.isSame(refreshedState) }
                    if (cachedState != null && cachedState == refreshedState) {
                        val cachedVaultItems = accountLockerDao.getVaultItems(
                            accountAddress = cachedState.accountAddress,
                            lockerAddress = cachedState.lockerAddress
                        )

                        cachedState.lockerAddress.takeIf { containsClaimableItems(cachedVaultItems) }
                    } else {
                        accountLockerDao.deleteVaultItems(refreshedState.accountAddress, refreshedState.lockerAddress)

                        val apiVaultItems = mutableListOf<AccountLockerVaultCollectionItem>()
                        stateApi.paginateAccountLockerVaultItems(
                            lockerAddress = refreshedState.lockerAddress,
                            accountAddress = refreshedState.accountAddress,
                            onPage = apiVaultItems::addAll
                        )
                        val refreshedVaultItems = apiVaultItems.mapNotNull { item ->
                            AccountLockerVaultItemEntity.from(
                                item = item,
                                accountAddress = refreshedState.accountAddress,
                                lockerAddress = refreshedState.lockerAddress
                            )
                        }

                        accountLockerDao.upsertVaultItems(refreshedVaultItems)

                        refreshedState.lockerAddress.takeIf {
                            containsClaimableItems(refreshedVaultItems)
                        }
                    }
                }.toSet()

                accountLockerDao.upsertTouchedAt(refreshedStates)

                claimableLockerAddresses
            }
        }
    }

    private fun containsClaimableItems(items: List<AccountLockerVaultItemEntity>): Boolean {
        return items.any { item -> item.amount.orZero() > 0.toDecimal192() || item.totalCount.or(0) > 0 }
    }
}
