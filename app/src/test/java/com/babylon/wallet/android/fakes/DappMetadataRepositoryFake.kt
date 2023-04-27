package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem

class DappMetadataRepositoryFake : DappMetadataRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return Result.Success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DappWithMetadata> {
        return Result.Success(
            DappWithMetadata(
                dAppDefinitionAddress = "dapp_address",
                nameItem = NameMetadataItem(name = "dApp")
            )
        )
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>> {
        return Result.Success(
            listOf(
                DappWithMetadata(
                    dAppDefinitionAddress = "dapp_address",
                    nameItem = NameMetadataItem(name = "dApp")
                )
            )
        )
    }
}
