package com.babylon.wallet.android.domain.repository.entity

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.*
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.repository.performHttpRequest
import javax.inject.Inject

class EntityRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : EntityRepository {

    override suspend fun entityDetails(address: String): Result<EntityDetailsResponse> {
        return performHttpRequest(call = {
            gatewayApi.entityDetails(EntityDetailsRequest(address))
        }, map = {
            it
        })
    }

    override suspend fun entityResources(address: String): Result<EntityResourcesResponse> {
        return performHttpRequest(call = {
            gatewayApi.entityResources(EntityResourcesRequest(address))
        }, map = {
            it
        })
    }

    override suspend fun entityOverview(addresses: List<String>): Result<EntityOverviewResponse> {
        return performHttpRequest(call = {
            gatewayApi.entityOverview(EntityOverviewRequest(addresses))
        }, map = {
            it
        })
    }

    override suspend fun entityMetadata(address: String, page: String?, limit: Int?): Result<EntityMetadataResponse> {
        return performHttpRequest(call = {
            gatewayApi.entityMetadata(EntityMetadataRequest(address, cursor = page, limit = limit))
        }, map = {
            it
        })
    }

}