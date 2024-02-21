package com.babylon.wallet.android.data.repository.stream

import com.babylon.wallet.android.data.gateway.apis.StreamApi
import com.babylon.wallet.android.data.gateway.generated.models.LedgerStateSelector
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequestAllOfManifestClassFilter
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsRequestEventFilterItem
import com.babylon.wallet.android.data.gateway.generated.models.StreamTransactionsResponse
import com.babylon.wallet.android.data.gateway.generated.models.TransactionDetailsOptIns
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.toManifestClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface StreamRepository {

    suspend fun getAccountHistory(
        accountAddress: String,
        filters: HistoryFilters,
        cursor: String?,
        stateVersion: Long? = null
    ): Result<StreamTransactionsResponse>

    suspend fun getAccountFirstTransaction(accountAddress: String): Result<StreamTransactionsResponse>

    companion object {
        const val PAGE_SIZE = 40
    }
}

class StreamRepositoryImpl @Inject constructor(
    private val streamApi: StreamApi,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : StreamRepository {
    override suspend fun getAccountHistory(
        accountAddress: String,
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

    override suspend fun getAccountFirstTransaction(accountAddress: String): Result<StreamTransactionsResponse> {
        return withContext(dispatcher) {
            runCatching {
                streamApi.streamTransactions(
                    StreamTransactionsRequest(
                        order = StreamTransactionsRequest.Order.asc,
                        fromLedgerState = LedgerStateSelector(stateVersion = 1),
                        accountsWithManifestOwnerMethodCalls = listOf(accountAddress),
                        limitPerPage = 1
                    )
                ).toResult().getOrThrow()
            }
        }
    }

    @Suppress("LongMethod")
    private fun buildStreamTransactionRequest(
        accountAddress: String,
        cursor: String?,
        filters: HistoryFilters,
        stateVersion: Long?
    ): StreamTransactionsRequest {
        val fromLedgerState = if (filters.start != null || stateVersion != null) {
            LedgerStateSelector(
                timestamp = filters.start?.toOffsetDateTime(),
                stateVersion = stateVersion
            )
        } else {
            null
        }
        val atLedgerState = if (filters.end != null || stateVersion != null) {
            LedgerStateSelector(
                timestamp = filters.end?.toOffsetDateTime(),
                stateVersion = stateVersion
            )
        } else {
            null
        }
        return StreamTransactionsRequest(
            optIns = TransactionDetailsOptIns(balanceChanges = true, affectedGlobalEntities = true),
            cursor = cursor,
            limitPerPage = StreamRepository.PAGE_SIZE,
            fromLedgerState = fromLedgerState,
            atLedgerState = atLedgerState,
            eventsFilter = filters.transactionType?.let { type ->
                when (type) {
                    HistoryFilters.TransactionType.DEPOSIT ->
                        listOf(
                            StreamTransactionsRequestEventFilterItem(
                                StreamTransactionsRequestEventFilterItem.Event.deposit,
                                emitterAddress = accountAddress
                            )
                        )
                    HistoryFilters.TransactionType.WITHDRAWAL ->
                        listOf(
                            StreamTransactionsRequestEventFilterItem(
                                StreamTransactionsRequestEventFilterItem.Event.withdrawal,
                                emitterAddress = accountAddress
                            )
                        )
                }
            },
            manifestClassFilter = filters.transactionClass?.toManifestClass()?.let {
                StreamTransactionsRequestAllOfManifestClassFilter(
                    it
                )
            },
            manifestResourcesFilter = if (filters.resources.isNotEmpty()) filters.resources.map { it.resourceAddress } else null,
            affectedGlobalEntitiesFilter = listOf(accountAddress),
            accountsWithManifestOwnerMethodCalls = if (filters.submittedBy == HistoryFilters.SubmittedBy.Me) {
                listOf(accountAddress)
            } else {
                null
            },
            accountsWithoutManifestOwnerMethodCalls = if (filters.submittedBy == HistoryFilters.SubmittedBy.ThirdParty) {
                listOf(accountAddress)
            } else {
                null
            }
        )
    }
}
