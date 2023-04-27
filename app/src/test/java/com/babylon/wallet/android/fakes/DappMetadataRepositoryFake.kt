package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem

class DappMetadataRepositoryFake : DappMetadataRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return Result.Success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DappMetadata> {
        return Result.Success(
            DappMetadata(
                nameItem = NameMetadataItem(name = "dApp"),
                dAppDefinitionMetadataItem = DAppDefinitionMetadataItem(address = "dapp_address")
            )
        )
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DappMetadata>> {
        return Result.Success(
            listOf(
                DappMetadata(
                    nameItem = NameMetadataItem(name = "dApp"),
                    dAppDefinitionMetadataItem = DAppDefinitionMetadataItem(address = "dapp_address")
                )
            )
        )
    }
}
