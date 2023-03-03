package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants

class DappMetadataRepositoryFake : DappMetadataRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return Result.Success(true)
    }

    override suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappMetadata> {
        return Result.Success(
            DappMetadata("dapp_address", mapOf(MetadataConstants.KEY_NAME to "dApp"))
        )
    }
}
