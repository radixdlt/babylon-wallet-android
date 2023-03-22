package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityNonFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.models.EntityResourcesRequest
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.FIVE_MINUTES
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration.NO_CACHE
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountResourcesSlim
import com.babylon.wallet.android.domain.model.toAccountResourceSlim
import javax.inject.Inject

// TODO translate from network models to domain models
interface EntityRepository {
    suspend fun entityDetails(
        address: String,
        isRefreshing: Boolean = true
    ): Result<EntityDetailsResponse>
    suspend fun getAccountResources(address: String, isRefreshing: Boolean): Result<AccountResourcesSlim>
    suspend fun entityOverview(addresses: List<String>): Result<EntityOverviewResponse>

    suspend fun entityMetadata(
        address: String,
        page: String? = null,
        limit: Int? = null
    ): Result<EntityMetadataResponse>

    suspend fun entityNonFungibles(
        address: String,
        page: String? = null,
        limit: Int? = null
    ): Result<EntityNonFungiblesResponse>

    suspend fun entityNonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null
    ): Result<EntityNonFungibleIdsResponse>
}

class EntityRepositoryImpl @Inject constructor(
    private val gatewayApi: GatewayApi,
    private val cache: HttpCache
) : EntityRepository {

    override suspend fun entityDetails(
        address: String,
        isRefreshing: Boolean
    ): Result<EntityDetailsResponse> {
        return gatewayApi.entityDetails(EntityDetailsRequest(address)).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else TimeoutDuration.ONE_MINUTE
            ),
            map = {
                it
            }
        )
    }

    override suspend fun getAccountResources(address: String, isRefreshing: Boolean): Result<AccountResourcesSlim> {
        return gatewayApi.entityResources(EntityResourcesRequest(address)).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (isRefreshing) NO_CACHE else FIVE_MINUTES
            ),
            map = { response -> response.toAccountResourceSlim() }
        )
    }

    override suspend fun entityOverview(addresses: List<String>): Result<EntityOverviewResponse> {
        return gatewayApi.entityOverview(EntityOverviewRequest(addresses)).execute(
            map = {
                it
            }
        )
    }

    override suspend fun entityMetadata(
        address: String,
        page: String?,
        limit: Int?
    ): Result<EntityMetadataResponse> {
        return gatewayApi.entityMetadata(EntityMetadataRequest(address, cursor = page, limit = limit))
            .execute(
                map = {
                    it
                }
            )
    }

    override suspend fun entityNonFungibles(
        address: String,
        page: String?,
        limit: Int?
    ): Result<EntityNonFungiblesResponse> {
        return gatewayApi.entityNonFungibles(EntityNonFungiblesRequest(address)).execute(
            map = {
                it
            }
        )
    }

    override suspend fun entityNonFungibleIds(
        address: String,
        page: String?,
        limit: Int?
    ): Result<EntityNonFungibleIdsResponse> {
        return gatewayApi.entityNonFungibleIds(EntityNonFungibleIdsRequest(address)).execute(
            map = {
                it
            }
        )
    }
}
