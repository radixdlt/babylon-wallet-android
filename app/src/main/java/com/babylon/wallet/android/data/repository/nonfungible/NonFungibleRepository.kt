package com.babylon.wallet.android.data.repository.nonfungible

import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer

// TODO translate from network models to domain models
interface NonFungibleRepository {

    suspend fun nonFungibleIds(
        address: String,
        page: String? = null,
        limit: Int? = null
    ): Result<NonFungibleTokenIdContainer>
}
