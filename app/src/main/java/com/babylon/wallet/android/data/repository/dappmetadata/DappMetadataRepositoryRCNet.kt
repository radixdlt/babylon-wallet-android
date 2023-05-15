package com.babylon.wallet.android.data.repository.dappmetadata

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem.AccountType
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DappMetadataRepositoryRCNet @Inject constructor(
    private val stateApi: StateApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cache: HttpCache
) : DappMetadataRepository {

    override suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> {
        error("Same implementation as ekninet")
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DappWithMetadata> {
        error("Same implementation as ekninet")
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>, // Not needed in rcnet
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>> = withContext(ioDispatcher) {
        stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = definitionAddresses
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (needMostRecentData) TimeoutDuration.NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                response.items.map { dAppResponse ->
                    val metadata = response.items.first().metadata.asMetadataStringMap()
                    DappWithMetadata(
                        dAppAddress = dAppResponse.address,
                        nameItem = metadata[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
                        descriptionItem = metadata[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
                        iconMetadataItem = metadata[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) },
                        relatedWebsitesItem = metadata[ExplicitMetadataKey.RELATED_WEBSITES.key]?.let { RelatedWebsiteMetadataItem(it) },
                        accountTypeItem = metadata[ExplicitMetadataKey.ACCOUNT_TYPE.key]?.let {
                            AccountTypeMetadataItem(type = AccountType.valueOf(it))
                        },
                        dAppDefinitionMetadataItem = metadata[ExplicitMetadataKey.DAPP_DEFINITION.key]?.let {
                            DAppDefinitionMetadataItem(it)
                        },
                        nonExplicitMetadataItems = metadata
                            .filterNot { entry ->
                                entry.key in ExplicitMetadataKey.forDapp.map { it.key }
                            }.map {
                                StringMetadataItem(it.key, it.value)
                            }
                    )
                }
            },
        )
    }
}
