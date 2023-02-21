package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungiblesRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesRequest
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountResourcesSlim
import com.babylon.wallet.android.domain.model.toAccountResourceSlim
import javax.inject.Inject

// TODO translate from network models to domain models
interface EntityRepository {
    suspend fun entityDetails(address: String): Result<EntityDetailsResponse>
    suspend fun getAccountResources(address: String): Result<AccountResourcesSlim>
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

class EntityRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : EntityRepository {

    override suspend fun entityDetails(address: String): Result<EntityDetailsResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.entityDetails(EntityDetailsRequest(address))
            },
            map = {
                it
            }
        )
    }

    override suspend fun getAccountResources(address: String): Result<AccountResourcesSlim> {
        return performHttpRequest(
            call = {
                gatewayApi.entityResources(EntityResourcesRequest(address))
            },
            map = { response ->
                response.toAccountResourceSlim()
            }
        )
    }

    override suspend fun entityOverview(addresses: List<String>): Result<EntityOverviewResponse> {
        return performHttpRequest(
            call = {
                gatewayApi.entityOverview(EntityOverviewRequest(addresses))
            },
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
        return performHttpRequest(
            call = {
                gatewayApi.entityMetadata(EntityMetadataRequest(address, cursor = page, limit = limit))
            },
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
        return performHttpRequest(
            call = {
                gatewayApi.entityNonFungibles(EntityNonFungiblesRequest(address))
            },
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
        return performHttpRequest(
            call = {
                gatewayApi.entityNonFungibleIds(EntityNonFungibleIdsRequest(address))
            },
            map = {
                it
            }
        )
    }
}
