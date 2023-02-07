package com.babylon.wallet.android.data.repository.nonfungible

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleLocalIdsRequest
import com.babylon.wallet.android.data.gateway.toDomainModel
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import javax.inject.Inject

class NonFungibleRepositoryImpl @Inject constructor(private val gatewayApi: GatewayApi) : NonFungibleRepository {

    override suspend fun nonFungibleIds(
        address: String,
        page: String?,
        limit: Int?
    ): Result<NonFungibleTokenIdContainer> {
        return performHttpRequest(
            call = {
                gatewayApi.nonFungibleIds(NonFungibleLocalIdsRequest(address))
            },
            map = {
                it.toDomainModel()
            }
        )
    }
}
