package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

interface EntityRepository {

    suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean = true
    ): Result<StateEntityDetailsResponse>

}

class EntityRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val cache: HttpCache
) : EntityRepository {

    override suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean
    ): Result<StateEntityDetailsResponse> {
        return stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = addresses,
                aggregationLevel = ResourceAggregationLevel.global
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
            ),
            map = { it }
        )
    }

}
