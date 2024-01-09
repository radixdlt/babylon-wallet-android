package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType

class DAppRepositoryFake : DAppRepository {

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String, wellKnownFileCheck: Boolean): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        needMostRecentData: Boolean,
        explicitMetadata: Set<ExplicitMetadataKey>
    ): Result<DApp> {
        return Result.success(
            DApp(
                dAppAddress = "dapp_address",
                metadata = listOf(
                    Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp", MetadataType.String)
                )
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
                    metadata = listOf(
                        Metadata.Primitive(ExplicitMetadataKey.NAME.key, "dApp", MetadataType.String)
                    )
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
