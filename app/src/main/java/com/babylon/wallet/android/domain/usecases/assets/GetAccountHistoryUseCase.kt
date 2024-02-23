package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.stream.StreamRepository
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.TransactionHistoryData
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.model.toTransactionClass
import message
import java.math.BigDecimal
import javax.inject.Inject

class GetAccountHistoryUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) {
    suspend operator fun invoke(
        accountAddress: String,
        transactionHistoryData: TransactionHistoryData? = null,
        filters: HistoryFilters
    ): Result<TransactionHistoryData> {
        return streamRepository.getAccountHistory(accountAddress, filters, transactionHistoryData?.nextCursorId).mapCatching { response ->
            val fungibleAddresses = response.items.map {
                it.balanceChanges?.fungibleBalanceChanges?.map { balanceChange ->
                    balanceChange.resourceAddress
                }.orEmpty().toSet() +
                    it.balanceChanges?.nonFungibleBalanceChanges?.map { balanceChange ->
                        balanceChange.resourceAddress
                    }.orEmpty().toSet()
            }.flatten().toSet()
            val nonFungibleAddresses = response.items.fold(mutableMapOf<String, Set<String>>()) { acc, item ->
                acc.apply {
                    item.balanceChanges?.nonFungibleBalanceChanges?.forEach { balanceChange ->
                        val nftCollectionAddress = balanceChange.resourceAddress
                        val nftLocalIds = balanceChange.added.toSet() + balanceChange.removed
                        if (containsKey(balanceChange.resourceAddress)) {
                            this[nftCollectionAddress] = this[nftCollectionAddress].orEmpty() + nftLocalIds
                        } else {
                            this[nftCollectionAddress] = nftLocalIds
                        }
                    }
                }
            }
            val assets = resolveAssetsFromAddressUseCase(fungibleAddresses, nonFungibleAddresses).getOrThrow()
            val items = response.items.map { item ->
                TransactionHistoryItem(
                    accountAddress = accountAddress,
                    txId = item.intentHash.orEmpty(),
                    feePaid = item.feePaid?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    balanceChanges = item.balanceChanges?.toDomainModel(assets = assets).orEmpty(),
                    transactionClass = item.manifestClasses?.firstOrNull()?.toTransactionClass(),
                    timestamp = item.confirmedAt?.toInstant(),
                    message = item.message?.message()
                )
            }
            TransactionHistoryData(
                stateVersion = response.ledgerState.stateVersion,
                items = transactionHistoryData?.items.orEmpty() + items,
                nextCursorId = response.nextCursor,
                currentFilters = filters
            )
        }
    }
}
