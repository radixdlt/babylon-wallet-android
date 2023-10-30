package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem

class DAppRepositoryFake : DAppRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String, wellKnownFileCheck: Boolean): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DApp> {
        return Result.success(
            DApp(
                dAppAddress = "dapp_address",
                nameItem = NameMetadataItem(name = "dApp")
            )
        )
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DApp>> {
        return Result.success(
            listOf(
                DApp(
                    dAppAddress = "dapp_address",
                    nameItem = NameMetadataItem(name = "dApp")
                )
            )
        )
    }

    override suspend fun getDAppResources(
        dAppMetadata: DApp,
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
