package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem

class DAppRepositoryFake : DAppRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String, wellKnownFileCheck: Boolean): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadata> {
        return Result.success(
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
        return Result.success(
            listOf(
                DAppWithMetadata(
                    dAppAddress = "dapp_address",
                    nameItem = NameMetadataItem(name = "dApp")
                )
            )
        )
    }

    override suspend fun getDAppResources(
        dAppMetadata: DAppWithMetadata,
        isRefreshing: Boolean
    ): Result<DAppResources> {
        return Result.success(
            DAppResources(
                fungibleResources = emptyList(),
                nonFungibleResources = emptyList()
            )
        )
    }
}
