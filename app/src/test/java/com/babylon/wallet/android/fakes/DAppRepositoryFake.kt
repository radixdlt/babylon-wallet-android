package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem

class DAppRepositoryFake : DAppRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return Result.Success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadata> {
        return Result.Success(
            DAppWithMetadata(
                dAppAddress = "dapp_address",
                nameItem = NameMetadataItem(name = "dApp")
            )
        )
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DAppWithMetadata>> {
        return Result.Success(
            listOf(
                DAppWithMetadata(
                    dAppAddress = "dapp_address",
                    nameItem = NameMetadataItem(name = "dApp")
                )
            )
        )
    }
}
