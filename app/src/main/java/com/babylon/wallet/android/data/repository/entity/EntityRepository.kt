package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import javax.inject.Inject

interface EntityRepository {

    suspend fun stateEntityDetails(
        addresses: List<String>,
        isRefreshing: Boolean = true
    ): Result<StateEntityDetailsResponse>

    suspend fun getNonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer>
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

    override suspend fun getNonFungibleIds(
        address: String,
        page: String?,
        limit: Int?,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer> {
        return stateApi.nonFungibleIds(StateNonFungibleIdsRequest(address)).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                NonFungibleTokenIdContainer(
                    ids = response.nonFungibleIds.items.map { it.nonFungibleId },
                    nextCursor = response.nonFungibleIds.nextCursor,
                    previousCursor = response.nonFungibleIds.previousCursor
                )
            }
        )
    }
}
