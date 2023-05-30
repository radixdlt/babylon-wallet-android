package com.babylon.wallet.android.data.repository.dappmetadata

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.extensions.fungibleResourceAddresses
import com.babylon.wallet.android.data.gateway.extensions.nonFungibleResourceAddresses
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem.AccountType
import com.babylon.wallet.android.domain.model.metadata.ClaimedEntitiesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem
import com.babylon.wallet.android.utils.isValidHttpsUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

class DappMetadataRepositoryRCNetImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val stateApi: StateApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cache: HttpCache
) : DappMetadataRepository {

    override suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> = withContext(ioDispatcher) {
        if (origin.isValidHttpsUrl()) {
            getDAppMetadata(
                definitionAddress = dAppDefinitionAddress,
                explicitMetadata = ExplicitMetadataKey.forDapp,
                needMostRecentData = false
            ).switchMap { gatewayMetadata ->
                when {
                    !gatewayMetadata.isDappDefinition -> {
                        Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.WrongAccountType))
                    }
                    !gatewayMetadata.isRelatedWith(origin) -> {
                        Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite))
                    }
                    else -> wellKnownFileMetadata(origin)
                }
            }.switchMap { wellKnownFileDAppDefinitions ->
                val isWellKnown = wellKnownFileDAppDefinitions.any { it == dAppDefinitionAddress }
                if (isWellKnown) {
                    Result.Success(true)
                } else {
                    Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownDefinitionAddress))
                }
            }
        } else {
            Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite))
        }
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadata> = getDAppsMetadata(
        definitionAddresses = listOf(definitionAddress),
        explicitMetadata = explicitMetadata,
        needMostRecentData = needMostRecentData
    ).map { dAppWithMetadataItems ->
        dAppWithMetadataItems.first()
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>, // Not needed in rcnet
        needMostRecentData: Boolean
    ): Result<List<DAppWithMetadata>> = withContext(ioDispatcher) {
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
                    DAppWithMetadata(
                        dAppAddress = dAppResponse.address,
                        nameItem = metadata[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
                        descriptionItem = metadata[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
                        iconMetadataItem = metadata[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) },
                        relatedWebsitesItem = metadata[ExplicitMetadataKey.RELATED_WEBSITES.key]?.let { RelatedWebsiteMetadataItem(it) },
                        claimedWebsiteItem = metadata[ExplicitMetadataKey.CLAIMED_WEBSITES.key]?.let { ClaimedWebsiteMetadataItem(it) },
                        claimedEntitiesItem = metadata[ExplicitMetadataKey.CLAIMED_ENTITIES.key]?.let { ClaimedEntitiesMetadataItem(it) },
                        accountTypeItem = metadata[ExplicitMetadataKey.ACCOUNT_TYPE.key]?.let {
                            val accountType = AccountType.values().find { value -> value.asString == it }
                            accountType?.let { type ->
                                AccountTypeMetadataItem(type = type)
                            }
                        },
                        dAppDefinitionMetadataItem = metadata[ExplicitMetadataKey.DAPP_DEFINITION.key]?.let {
                            DAppDefinitionMetadataItem(it)
                        },
                        nonExplicitMetadataItems = metadata
                            .filterNot { entry ->
                                entry.key in ExplicitMetadataKey.forDapp.map { it.key }
                            }.map {
                                StringMetadataItem(it.key, it.value)
                            },
                        associatedFungibleResourceAddresses = dAppResponse.fungibleResourceAddresses,
                        associatedNonFungibleResourceAddresses = dAppResponse.nonFungibleResourceAddresses,
                        fungibleResources = dAppResponse.fungibleResources?.items.orEmpty(),
                        nonFungibleResources = dAppResponse.nonFungibleResources?.items.orEmpty()
                    )
                }
            }
        )
    }

    private suspend fun wellKnownFileMetadata(
        origin: String
    ): Result<List<String>> {
        return withContext(ioDispatcher) {
            buildApi<DAppDefinitionApi>(
                baseUrl = origin,
                okHttpClient = okHttpClient,
                jsonConverterFactory = jsonConverterFactory
            ).wellKnownDAppDefinition().execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = TimeoutDuration.FIVE_MINUTES
                ),
                map = { response -> response.dApps.map { it.dAppDefinitionAddress } },
                error = {
                    DappRequestException(DappRequestFailure.DappVerificationFailure.RadixJsonNotFound)
                }
            )
        }
    }
}
