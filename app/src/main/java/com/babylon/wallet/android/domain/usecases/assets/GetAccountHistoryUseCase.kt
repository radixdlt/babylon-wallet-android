package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import com.babylon.wallet.android.data.repository.stream.StreamRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.TransactionHistoryData
import com.babylon.wallet.android.domain.model.toDomainModel
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAccountHistoryUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun getHistory(
        accountAddress: String,
        filters: HistoryFilters
    ): Result<TransactionHistoryData> {
        return streamRepository.getAccountHistory(accountAddress, filters, null).mapCatching { response ->
            val assetsAddresses = resolveResponseAssetAddresses(response)
            val assets = resolveAssetsFromAddressUseCase(assetsAddresses.first, assetsAddresses.second).getOrThrow()
            val items = response.items.map { item -> item.toDomainModel(accountAddress, assets) }
            TransactionHistoryData(
                stateVersion = response.ledgerState.stateVersion,
                prevCursorId = null,
                nextCursorId = response.nextCursor,
                filters = filters,
                items = items
            )
        }
    }

    suspend fun getHistoryChunk(
        accountAddress: String,
        filters: HistoryFilters
    ): Result<TransactionHistoryData> {
        return withContext(ioDispatcher) {
            coroutineScope {
                val forward = async {
                    streamRepository.getAccountHistory(
                        accountAddress = accountAddress,
                        filters = filters.copy(sortOrder = HistoryFilters.SortOrder.Asc),
                        cursor = null
                    )
                }
                val back = async {
                    streamRepository.getAccountHistory(
                        accountAddress = accountAddress,
                        filters = filters.copy(sortOrder = HistoryFilters.SortOrder.Desc),
                        cursor = null
                    )
                }
                val backResponse = back.await().getOrNull()
                val forwardResponse = forward.await().getOrNull()
                if (backResponse != null && forwardResponse != null) {
                    val backAssets = resolveResponseAssetAddresses(backResponse)
                    val forwardAssets = resolveResponseAssetAddresses(forwardResponse)
                    val fungibleAddresses = backAssets.first + forwardAssets.first
                    val nonFungibleAddresses = backAssets.second.toMutableMap().apply {
                        forwardAssets.second.forEach { (key, value) ->
                            if (containsKey(key)) {
                                this[key] = this[key].orEmpty() + value
                            } else {
                                this[key] = value
                            }
                        }
                    }
                    val assets = resolveAssetsFromAddressUseCase(fungibleAddresses, nonFungibleAddresses).getOrThrow()
                    val items = (forwardResponse.items + backResponse.items)
                        .distinctBy {
                            it.intentHash
                        }.map { committedTransactionInfo ->
                            committedTransactionInfo.toDomainModel(accountAddress, assets)
                        }.sortedByDescending {
                            it.timestamp
                        }
                    Result.success(
                        TransactionHistoryData(
                            stateVersion = backResponse.ledgerState.stateVersion,
                            prevCursorId = forwardResponse.nextCursor,
                            nextCursorId = backResponse.nextCursor,
                            filters = filters,
                            items = items
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to fetch history"))
                }
            }
        }
    }

    private fun resolveResponseAssetAddresses(
        response: StreamTransactionsResponse
    ): Pair<Set<String>, MutableMap<String, Set<NonFungibleLocalId>>> {
        val fungibleAddresses = response.items.map {
            it.balanceChanges?.fungibleBalanceChanges?.map { balanceChange ->
                balanceChange.resourceAddress
            }.orEmpty().toSet() +
                it.balanceChanges?.nonFungibleBalanceChanges?.map { balanceChange ->
                    balanceChange.resourceAddress
                }.orEmpty().toSet()
        }.flatten().toSet()
        val nonFungibleAddresses = response.items.fold(mutableMapOf<String, Set<NonFungibleLocalId>>()) { acc, item ->
            acc.apply {
                item.balanceChanges?.nonFungibleBalanceChanges?.forEach { balanceChange ->
                    val nftCollectionAddress = balanceChange.resourceAddress
                    val nftLocalIds = (balanceChange.added.toSet() + balanceChange.removed).map {
                        NonFungibleLocalId.init(it)
                    }.toSet()
                    if (containsKey(nftCollectionAddress)) {
                        this[nftCollectionAddress] = this[nftCollectionAddress].orEmpty() + nftLocalIds
                    } else {
                        this[nftCollectionAddress] = nftLocalIds
                    }
                }
            }
        }
        return fungibleAddresses to nonFungibleAddresses
    }

    suspend fun loadMore(
        accountAddress: String,
        transactionHistoryData: TransactionHistoryData,
        prepend: Boolean = false
    ): Result<TransactionHistoryData> {
        val nextCursor = if (prepend) transactionHistoryData.prevCursorId else transactionHistoryData.nextCursorId
        val filters = if (prepend) {
            transactionHistoryData.filters.copy(sortOrder = HistoryFilters.SortOrder.Asc)
        } else {
            transactionHistoryData.filters
        }
        return streamRepository.getAccountHistory(accountAddress, filters, nextCursor).mapCatching { response ->
            val assetsAddresses = resolveResponseAssetAddresses(response)
            val assets = resolveAssetsFromAddressUseCase(assetsAddresses.first, assetsAddresses.second).getOrThrow()
            val items = response.items.map { item -> item.toDomainModel(accountAddress, assets) }
            if (prepend) {
                transactionHistoryData.prepend(items, response.nextCursor)
            } else {
                transactionHistoryData.append(items, response.nextCursor)
            }
        }
    }
}
