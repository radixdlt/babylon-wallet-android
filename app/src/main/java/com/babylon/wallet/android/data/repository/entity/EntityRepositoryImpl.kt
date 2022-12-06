package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewRequest
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityResourcesRequest
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountResourcesSlim
import com.babylon.wallet.android.domain.model.toAccountResourceSlim
import javax.inject.Inject

class EntityRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : EntityRepository {

    override suspend fun entityDetails(address: String): Result<EntityDetailsResponse> {
        return performHttpRequest(call = {
            gatewayApi.entityDetails(EntityDetailsRequest(address))
        }, map = {
                it
            })
    }

    override suspend fun getAccountResources(address: String): Result<AccountResourcesSlim> {
        return performHttpRequest(call = {
            gatewayApi.entityResources(EntityResourcesRequest(address))
        }, map = { response ->
                response.toAccountResourceSlim()
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
