package com.babylon.wallet.android.data.repository.nonfungible

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleIdsRequest
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.FIVE_MINUTES
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import javax.inject.Inject

interface NonFungibleRepository {

    suspend fun nonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer>
}

class NonFungibleRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val httpCache: HttpCache
) : NonFungibleRepository {

    override suspend fun nonFungibleIds(
        address: String,
        page: String?,
        limit: Int?,
        isRefreshing: Boolean
    ): Result<NonFungibleTokenIdContainer> {
        return stateApi.nonFungibleIds(StateNonFungibleIdsRequest(address)).execute(
            cacheParameters = CacheParameters(
                httpCache = httpCache,
                timeoutDuration = if (isRefreshing) NO_CACHE else FIVE_MINUTES
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
