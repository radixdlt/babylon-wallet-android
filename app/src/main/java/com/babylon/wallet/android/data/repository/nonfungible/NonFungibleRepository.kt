package com.babylon.wallet.android.data.repository.nonfungible

import com.babylon.wallet.android.data.gateway.GatewayApi
import com.babylon.wallet.android.data.gateway.generated.model.NonFungibleIdsRequest
import com.babylon.wallet.android.data.gateway.toDomainModel
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import javax.inject.Inject

// TODO translate from network models to domain models
interface NonFungibleRepository {

    suspend fun nonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null
    ): Result<NonFungibleTokenIdContainer>
}

class NonFungibleRepositoryImpl @Inject constructor(
    private val gatewayApi: GatewayApi
) : NonFungibleRepository {

    override suspend fun nonFungibleIds(
        address: String,
        page: String?,
        limit: Int?
    ): Result<NonFungibleTokenIdContainer> {
        return performHttpRequest(
            call = {
                gatewayApi.nonFungibleIds(NonFungibleIdsRequest(address))
            },
            map = {
                it.toDomainModel()
            }
        )
    }
}
