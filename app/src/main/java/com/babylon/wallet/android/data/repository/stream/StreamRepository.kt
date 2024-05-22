package com.babylon.wallet.android.data.repository.stream

import com.babylon.wallet.android.data.gateway.apis.StreamApi
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequestAllOfManifestClassFilter
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequestEventFilterItem
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionDetailsOptIns
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.toManifestClass
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface StreamRepository {

    suspend fun getAccountHistory(
        accountAddress: AccountAddress,
        filters: HistoryFilters,
        cursor: String?,
        stateVersion: Long? = null
    ): Result<StreamTransactionsResponse>

    suspend fun getAccountFirstTransactionDate(accountAddress: AccountAddress): Result<StreamTransactionsResponse>

    companion object {
        const val PAGE_SIZE = 20
    }
}

class StreamRepositoryImpl @Inject constructor(
    private val streamApi: StreamApi,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : StreamRepository {
    override suspend fun getAccountHistory(
        accountAddress: AccountAddress,
        filters: HistoryFilters,
        cursor: String?,
        stateVersion: Long?
    ): Result<StreamTransactionsResponse> {
        return withContext(dispatcher) {
            runCatching {
                val request = buildStreamTransactionRequest(accountAddress, cursor, filters, stateVersion)
                streamApi.streamTransactions(request).toResult().getOrThrow()
            }
        }
    }

    override suspend fun getAccountFirstTransactionDate(accountAddress: AccountAddress): Result<StreamTransactionsResponse> {
        return withContext(dispatcher) {
            runCatching {
                streamApi.streamTransactions(
                    StreamTransactionsRequest(
                        order = StreamTransactionsRequest.Order.Asc,
                        fromLedgerState = LedgerStateSelector(stateVersion = 1),
                        affectedGlobalEntitiesFilter = listOf(accountAddress.string),
                        limitPerPage = 1
                    )
                ).toResult().getOrThrow()
            }
        }
    }

    @Suppress("LongMethod")
    private fun buildStreamTransactionRequest(
        accountAddress: AccountAddress,
        cursor: String?,
        filters: HistoryFilters,
        stateVersion: Long?
    ): StreamTransactionsRequest {
        val ledgerState = if (stateVersion != null || filters.start != null) {
            LedgerStateSelector(
                timestamp = filters.start?.toOffsetDateTime(),
                stateVersion = stateVersion
            )
        } else {
            null
        }
        val genesisSelector = filters.genesisTxDate?.let { genesisDate ->
            LedgerStateSelector(timestamp = genesisDate.toOffsetDateTime())
        }
        val ascending = filters.sortOrder == HistoryFilters.SortOrder.Asc
        return StreamTransactionsRequest(
            optIns = TransactionDetailsOptIns(balanceChanges = true),
            cursor = cursor,
            limitPerPage = StreamRepository.PAGE_SIZE,
            atLedgerState = if (ascending) null else ledgerState,
            fromLedgerState = if (ascending) ledgerState else genesisSelector,
            order = when (filters.sortOrder) {
                HistoryFilters.SortOrder.Asc -> StreamTransactionsRequest.Order.Asc
                HistoryFilters.SortOrder.Desc -> StreamTransactionsRequest.Order.Desc
            },
            eventsFilter = filters.transactionType?.let { type ->
                when (type) {
                    HistoryFilters.TransactionType.DEPOSIT ->
                        listOf(
                            StreamTransactionsRequestEventFilterItem(
                                StreamTransactionsRequestEventFilterItem.Event.Deposit,
                                emitterAddress = accountAddress.string
                            )
                        )

                    HistoryFilters.TransactionType.WITHDRAWAL ->
                        listOf(
                            StreamTransactionsRequestEventFilterItem(
                                StreamTransactionsRequestEventFilterItem.Event.Withdrawal,
                                emitterAddress = accountAddress.string
                            )
                        )
                }
            },
            manifestClassFilter = filters.transactionClass?.toManifestClass()?.let {
                StreamTransactionsRequestAllOfManifestClassFilter(
                    propertyClass = it,
                    matchOnlyMostSpecific = true
                )
            },
            manifestResourcesFilter = filters.resource?.let { listOf(it.address.string) },
            affectedGlobalEntitiesFilter = listOf(accountAddress.string)
        )
    }
}
