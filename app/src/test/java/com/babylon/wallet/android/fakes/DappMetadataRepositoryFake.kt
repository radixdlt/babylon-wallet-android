package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants

class DappMetadataRepositoryFake : DappMetadataRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return Result.Success(true)
    }

    override suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappWithMetadata> {
        return Result.Success(
            DappWithMetadata("dapp_address", mapOf(MetadataConstants.KEY_NAME to "dApp"))
        )
    }

    override suspend fun getDappsMetadata(
        defitnionAddresses: List<String>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>> {
        return Result.Success(
            listOf(
                DappWithMetadata("dapp_address", mapOf(MetadataConstants.KEY_NAME to "dApp"))
            )
        )
    }
}
