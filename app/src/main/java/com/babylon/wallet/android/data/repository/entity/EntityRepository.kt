package com.babylon.wallet.android.data.repository.entity

import com.babylon.wallet.android.data.gateway.generated.model.EntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityMetadataResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungibleIdsResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityNonFungiblesResponse
import com.babylon.wallet.android.data.gateway.generated.model.EntityOverviewResponse
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountResourcesSlim

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
